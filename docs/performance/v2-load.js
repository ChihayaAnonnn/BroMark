import http from 'k6/http';
import exec from 'k6/execution';
import { SharedArray } from 'k6/data';
import { Counter } from 'k6/metrics';

const config=JSON.parse(open(__ENV.CONFIG || '/scripts/v2-load-config.json'));
const users=new SharedArray('identities',()=>Object.entries(JSON.parse(open(__ENV.TOKEN_FILE))));
const seed=JSON.parse(open(__ENV.SEED_FILE));
const sent=new Counter('v2_sent'),accepted=new Counter('v2_accepted'),rejected=new Counter('v2_rejected');
const scenario=__ENV.SCENARIO || 'mixed';
const fixtures=__ENV.NOTIFICATION_FILE ? JSON.parse(open(__ENV.NOTIFICATION_FILE)) : [];
if(!config.scenarios.includes(scenario)) throw new Error('unknown scenario');
export const options={scenarios:{load:{executor:'constant-arrival-rate',
  rate:Number(__ENV.RATE || 100),timeUnit:'1s',duration:__ENV.DURATION || '600s',
  preAllocatedVUs:scenario==='hot-user'?Number(__ENV.HOT_USER_CONCURRENCY || 2):config.preAllocatedVUs,maxVUs:scenario==='hot-user'?Number(__ENV.HOT_USER_CONCURRENCY || 2):config.maxVUs}},
  discardResponseBodies:false,summaryTrendStats:['p(50)','p(95)','p(99)','max'],
  systemTags:['status','method','name','scenario','expected_response']};

export default function() {
  const iteration=exec.scenario.iterationInTest;
  const userIndex=scenario==='signin-first' ? iteration : scenario==='hot-user' ? 0 : iteration%users.length;
  if(userIndex>=users.length) throw new Error('insufficient unique identities for first signin');
  const [user,token]=users[userIndex];
  const requestId=`${__ENV.RUN_ID || seed.runId}:${exec.scenario.name}:${iteration}`;
  let operation='draw';
  if(scenario==='mixed') { const n=iteration%10; operation=n<7?'query':n===7?'result':n===8?'exchange':'draw'; }
  if(scenario.startsWith('signin')) operation='signin';
  if(scenario==='payment-create') operation='payment';
  if(scenario==='payment-replay') operation='notification';
  let method='POST',path='/api/v2/draws',body={requestId,activityId:seed.activityId};
  if(operation==='query') {method='GET';path=`/api/v2/activities/${seed.activityId}`;body=null;}
  if(operation==='result') {
    if(!seed.resultOrders || !seed.resultOrders[user]) throw new Error('pre-created result order per identity required');
    method='GET';path='/api/v2/draws/'+seed.resultOrders[user];body=null;
  }
  if(operation==='exchange') {path='/api/v2/credit/exchanges';body={requestId,sku:seed.sku,configVersion:seed.version};}
  if(operation==='signin') {path='/api/v2/rebates/sign-ins';body={requestId,targetDate:__ENV.BUSINESS_DATE};}
  if(operation==='payment') {path='/api/v2/payment/orders';body={requestId,productId:'sku',provider:'sandbox'};}
  let headers={'Authorization':'Bearer '+token,'Content-Type':'application/json'};
  if(operation==='notification') {
    if(!fixtures.length) throw new Error('signed notification fixture required');
    const fixture=fixtures[iteration%fixtures.length];
    path='/api/v2/payment/notifications/sandbox';body=fixture.raw;
    headers={'Content-Type':'application/json','x-sandbox-signature':fixture.signature};
  }
  sent.add(1);
  const response=http.request(method,__ENV.BASE_URL+path,typeof body==='string'?body:body===null?null:JSON.stringify(body),
    {headers,tags:{name:operation},timeout:'6s'});
  let value={};try {value=response.json();}catch(e){}
  if(response.status>=200 && response.status<300 && value.state!=='REJECTED') accepted.add(1);
  else rejected.add(1);
  // One iteration, one primary request. Polling is a separate program and query budget.
  console.log(JSON.stringify({requestId,user,operation,status:response.status,orderId:value.orderId || null,
    state:value.state || null,latencyMs:response.timings.duration,observedAt:Date.now()/1000}));
}
