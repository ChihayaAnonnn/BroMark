/*
 Navicat Premium Dump SQL

 Source Server         : bm-mysql
 Source Server Type    : MySQL
 Source Server Version : 90300 (9.3.0)
 Source Host           : 172.31.74.95:13306
 Source Schema         : big_market_01

 Target Server Type    : MySQL
 Target Server Version : 90300 (9.3.0)
 File Encoding         : 65001

 Date: 12/09/2025 13:31:54
*/
create database big_market_01;
use big_market_01;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for raffle_activity_account
-- ----------------------------
DROP TABLE IF EXISTS `raffle_activity_account`;
CREATE TABLE `raffle_activity_account`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `total_count` int NOT NULL COMMENT '总次数',
  `total_count_surplus` int NOT NULL COMMENT '总次数-剩余',
  `day_count` int NOT NULL COMMENT '日次数',
  `day_count_surplus` int NOT NULL COMMENT '日次数-剩余',
  `month_count` int NOT NULL COMMENT '月次数',
  `month_count_surplus` int NOT NULL COMMENT '月次数-剩余',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_user_id_activity_id`(`user_id` ASC, `activity_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 911 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抽奖活动账户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of raffle_activity_account
-- ----------------------------
INSERT INTO `raffle_activity_account` VALUES (8, 'jay', 100301, 1200, 1092, 1200, 1092, 1200, 1092, '2025-09-11 15:35:32', '2025-09-12 12:14:33');
INSERT INTO `raffle_activity_account` VALUES (649, 'fvpki7.keb22', 100301, 1000, 992, 1000, 992, 1000, 992, '2025-09-11 17:08:27', '2025-09-11 20:40:32');
INSERT INTO `raffle_activity_account` VALUES (651, 'ixjlj1_fgs', 100301, 1000, 993, 1000, 993, 1000, 993, '2025-09-11 17:16:23', '2025-09-11 20:40:33');
INSERT INTO `raffle_activity_account` VALUES (652, 'v9bmdm98', 100301, 1000, 997, 1000, 997, 1000, 997, '2025-09-11 19:15:11', '2025-09-11 20:40:34');
INSERT INTO `raffle_activity_account` VALUES (653, 'l7nrqi39', 100301, 1000, 998, 1000, 998, 1000, 998, '2025-09-11 19:16:09', '2025-09-11 20:40:35');
INSERT INTO `raffle_activity_account` VALUES (654, 'tbfv9r_l7q84', 100301, 1000, 998, 1000, 998, 1000, 998, '2025-09-11 19:16:09', '2025-09-11 20:40:35');
INSERT INTO `raffle_activity_account` VALUES (655, 'jwupu5.gjv', 100301, 1000, 998, 1000, 998, 1000, 998, '2025-09-11 19:16:09', '2025-09-11 20:40:36');
INSERT INTO `raffle_activity_account` VALUES (656, 'lh7lnf.iul17', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:09', '2025-09-11 20:40:37');
INSERT INTO `raffle_activity_account` VALUES (657, 'p72k3d55', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:09', '2025-09-11 20:40:37');
INSERT INTO `raffle_activity_account` VALUES (658, 'izhfoz52', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:09', '2025-09-11 20:40:38');
INSERT INTO `raffle_activity_account` VALUES (659, 'jlbi46_jew', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:10', '2025-09-11 20:40:39');
INSERT INTO `raffle_activity_account` VALUES (660, 'vdak5x51', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:10', '2025-09-11 20:40:39');
INSERT INTO `raffle_activity_account` VALUES (661, 'k1zsbt76', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:10', '2025-09-11 20:40:40');
INSERT INTO `raffle_activity_account` VALUES (662, 'v9blnf12', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:10', '2025-09-11 20:40:41');
INSERT INTO `raffle_activity_account` VALUES (663, 'ggqryd_skd44', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:10', '2025-09-11 20:40:42');
INSERT INTO `raffle_activity_account` VALUES (664, 'fsekfk.jh485', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:10', '2025-09-11 20:40:42');
INSERT INTO `raffle_activity_account` VALUES (665, 'k8qkfk86', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:11', '2025-09-11 20:40:43');
INSERT INTO `raffle_activity_account` VALUES (666, 'pd6fvp_kd762', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:11', '2025-09-11 20:40:44');
INSERT INTO `raffle_activity_account` VALUES (667, 'irescp24', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:11', '2025-09-11 20:40:44');
INSERT INTO `raffle_activity_account` VALUES (668, 'lm1_p1k', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:11', '2025-09-11 20:40:45');
INSERT INTO `raffle_activity_account` VALUES (669, 'tgmo3v_tg9', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:11', '2025-09-11 20:40:46');
INSERT INTO `raffle_activity_account` VALUES (670, 'sf0ppm47', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:11', '2025-09-11 20:40:46');
INSERT INTO `raffle_activity_account` VALUES (671, 'pgknz9.q5795', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:11', '2025-09-11 20:40:47');
INSERT INTO `raffle_activity_account` VALUES (672, 'gs2m5650', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:11', '2025-09-11 20:40:48');
INSERT INTO `raffle_activity_account` VALUES (673, 'jzzgm341', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:11', '2025-09-11 20:40:48');
INSERT INTO `raffle_activity_account` VALUES (674, 'k8qscp_glx72', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:11', '2025-09-11 20:40:49');
INSERT INTO `raffle_activity_account` VALUES (675, 'i2xmy224', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:12', '2025-09-11 20:40:50');
INSERT INTO `raffle_activity_account` VALUES (676, 'nlrlqi_j7y', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:12', '2025-09-11 20:40:50');
INSERT INTO `raffle_activity_account` VALUES (677, 'ryilj149', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:12', '2025-09-11 20:40:51');
INSERT INTO `raffle_activity_account` VALUES (678, 'mzitue_ol733', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:12', '2025-09-11 20:40:52');
INSERT INTO `raffle_activity_account` VALUES (679, 'ixjlj1_tqo', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:12', '2025-09-11 20:40:52');
INSERT INTO `raffle_activity_account` VALUES (680, 'lm1i2v83', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:12', '2025-09-11 20:40:53');
INSERT INTO `raffle_activity_account` VALUES (681, 'ixjit6.laj61', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:13', '2025-09-11 20:40:54');
INSERT INTO `raffle_activity_account` VALUES (682, 'm97k5q.ny968', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:13', '2025-09-11 20:40:54');
INSERT INTO `raffle_activity_account` VALUES (683, 'ui8_n1l68', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:13', '2025-09-11 20:40:55');
INSERT INTO `raffle_activity_account` VALUES (684, 'tt6kfy43', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:13', '2025-09-11 20:40:56');
INSERT INTO `raffle_activity_account` VALUES (685, 'k1z74', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:13', '2025-09-11 20:40:56');
INSERT INTO `raffle_activity_account` VALUES (686, 'oq3kgn38', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:13', '2025-09-11 20:40:57');
INSERT INTO `raffle_activity_account` VALUES (687, 'kgs_man44', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:13', '2025-09-11 20:40:58');
INSERT INTO `raffle_activity_account` VALUES (688, 'i1csbt_gqo91', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:14', '2025-09-11 20:40:58');
INSERT INTO `raffle_activity_account` VALUES (689, 'ts5hvb.skx', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:14', '2025-09-11 20:40:59');
INSERT INTO `raffle_activity_account` VALUES (690, 'm8eiuk_fl0', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:14', '2025-09-11 20:41:00');
INSERT INTO `raffle_activity_account` VALUES (691, 'k8qkfk.sks1', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:14', '2025-09-11 20:41:00');
INSERT INTO `raffle_activity_account` VALUES (692, 'ui865', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:14', '2025-09-11 20:41:01');
INSERT INTO `raffle_activity_account` VALUES (693, 'i2xmy2_fj6', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:15', '2025-09-11 20:41:02');
INSERT INTO `raffle_activity_account` VALUES (694, 'izhpd6_jwt19', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:15', '2025-09-11 20:41:03');
INSERT INTO `raffle_activity_account` VALUES (695, 'tw6my286', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:15', '2025-09-11 20:41:03');
INSERT INTO `raffle_activity_account` VALUES (696, 'pa8kfk35', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:15', '2025-09-11 20:41:04');
INSERT INTO `raffle_activity_account` VALUES (697, 'tt6kfy.i809', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:15', '2025-09-11 20:41:05');
INSERT INTO `raffle_activity_account` VALUES (698, 'i1cvdk88', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:15', '2025-09-11 20:41:05');
INSERT INTO `raffle_activity_account` VALUES (699, 'mawfmn82', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:15', '2025-09-11 20:41:06');
INSERT INTO `raffle_activity_account` VALUES (700, 'fmnrmi_ff1', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:16', '2025-09-11 20:41:07');
INSERT INTO `raffle_activity_account` VALUES (701, 'me4nzu_ph932', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:16', '2025-09-11 20:41:07');
INSERT INTO `raffle_activity_account` VALUES (702, 'tbfv9r_ssq', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:16', '2025-09-11 20:41:08');
INSERT INTO `raffle_activity_account` VALUES (703, 'v9rlnf.gbl49', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:16', '2025-09-11 20:41:09');
INSERT INTO `raffle_activity_account` VALUES (704, 'k5mhm1_ky2', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:16', '2025-09-11 20:41:09');
INSERT INTO `raffle_activity_account` VALUES (705, 'k5qkfk.qap8', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:16', '2025-09-11 20:41:10');
INSERT INTO `raffle_activity_account` VALUES (706, 'pu9ggq.gc3', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:17', '2025-09-11 20:41:11');
INSERT INTO `raffle_activity_account` VALUES (707, 'rmftt492', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:17', '2025-09-11 20:41:11');
INSERT INTO `raffle_activity_account` VALUES (708, 'tt6kfy.lly', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:17', '2025-09-11 20:41:12');
INSERT INTO `raffle_activity_account` VALUES (709, 'ui859', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:17', '2025-09-11 20:41:13');
INSERT INTO `raffle_activity_account` VALUES (710, 'v9ru724', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:17', '2025-09-11 20:41:13');
INSERT INTO `raffle_activity_account` VALUES (711, 'hm1nuy43', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:17', '2025-09-11 20:41:14');
INSERT INTO `raffle_activity_account` VALUES (712, 'nlrlqi66', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:17', '2025-09-11 20:41:15');
INSERT INTO `raffle_activity_account` VALUES (713, 'fevfoz.gne23', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:17', '2025-09-11 20:41:16');
INSERT INTO `raffle_activity_account` VALUES (714, 'k8qi48.pip39', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:17', '2025-09-11 20:41:17');
INSERT INTO `raffle_activity_account` VALUES (715, 'mzitue.m2hgkw', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:17', '2025-09-11 20:41:17');
INSERT INTO `raffle_activity_account` VALUES (716, 'izhlq4.lpvfim58', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:18', '2025-09-11 20:41:18');
INSERT INTO `raffle_activity_account` VALUES (717, 'lh7lnf96', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:18', '2025-09-11 20:41:19');
INSERT INTO `raffle_activity_account` VALUES (718, 'fh5q31.ljf', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:18', '2025-09-11 20:41:20');
INSERT INTO `raffle_activity_account` VALUES (719, 'gs2k34_ny930', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:18', '2025-09-11 20:41:21');
INSERT INTO `raffle_activity_account` VALUES (720, 'mawtue37', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:18', '2025-09-11 20:41:21');
INSERT INTO `raffle_activity_account` VALUES (721, 'jwupu524', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:18', '2025-09-11 20:41:22');
INSERT INTO `raffle_activity_account` VALUES (722, 'pu9ggq_h5q81', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:18', '2025-09-11 20:41:23');
INSERT INTO `raffle_activity_account` VALUES (723, 'hm1nuy_frz89', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:19', '2025-09-11 20:41:24');
INSERT INTO `raffle_activity_account` VALUES (724, 'tgmo3v.tfa99', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:19', '2025-09-11 20:41:25');
INSERT INTO `raffle_activity_account` VALUES (725, 'k1zu0c.gbf58', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:19', '2025-09-11 20:41:26');
INSERT INTO `raffle_activity_account` VALUES (726, 'id7mft99', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:19', '2025-09-11 20:41:26');
INSERT INTO `raffle_activity_account` VALUES (727, 'tbfnuy58', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:19', '2025-09-11 20:41:27');
INSERT INTO `raffle_activity_account` VALUES (728, 'ffxlmf_l9v84', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:19', '2025-09-11 20:41:28');
INSERT INTO `raffle_activity_account` VALUES (729, 'fmnrmi_ptd', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:19', '2025-09-11 20:41:29');
INSERT INTO `raffle_activity_account` VALUES (730, 'tt4lj1_vix54', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:19', '2025-09-11 20:41:30');
INSERT INTO `raffle_activity_account` VALUES (731, 'irescp12', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:20', '2025-09-11 20:41:30');
INSERT INTO `raffle_activity_account` VALUES (732, 'ttbtw992', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:20', '2025-09-11 20:41:31');
INSERT INTO `raffle_activity_account` VALUES (733, 'fozmy264', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:20', '2025-09-11 20:41:32');
INSERT INTO `raffle_activity_account` VALUES (734, 'rydkjv.g3k46', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:20', '2025-09-11 20:41:33');
INSERT INTO `raffle_activity_account` VALUES (735, 'fgwuih10', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:20', '2025-09-11 20:41:33');
INSERT INTO `raffle_activity_account` VALUES (736, 'gs2m5634', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:21', '2025-09-11 20:41:34');
INSERT INTO `raffle_activity_account` VALUES (737, 'mawnuy3', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:21', '2025-09-11 20:41:35');
INSERT INTO `raffle_activity_account` VALUES (738, 'mawfmn_rlk', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:21', '2025-09-11 20:41:36');
INSERT INTO `raffle_activity_account` VALUES (739, 'izhfoz96', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:21', '2025-09-11 20:41:37');
INSERT INTO `raffle_activity_account` VALUES (740, 'p72k3d.rvz', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:21', '2025-09-11 20:41:37');
INSERT INTO `raffle_activity_account` VALUES (741, 'izhpd6_lxi', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:21', '2025-09-11 20:41:38');
INSERT INTO `raffle_activity_account` VALUES (742, 'jzzgm374', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:22', '2025-09-11 20:41:39');
INSERT INTO `raffle_activity_account` VALUES (743, 'i1cuji.nev32', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:22', '2025-09-11 20:41:40');
INSERT INTO `raffle_activity_account` VALUES (744, 'lj1ljf_g8b10', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:22', '2025-09-11 20:41:41');
INSERT INTO `raffle_activity_account` VALUES (745, 'hm1nuy.lqd', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:22', '2025-09-11 20:41:41');
INSERT INTO `raffle_activity_account` VALUES (746, 'k1zggq.o34', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:22', '2025-09-11 20:41:42');
INSERT INTO `raffle_activity_account` VALUES (747, 'v9rmdm_gp82', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:22', '2025-09-11 20:41:43');
INSERT INTO `raffle_activity_account` VALUES (748, 'o97scp88', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:23', '2025-09-11 20:41:44');
INSERT INTO `raffle_activity_account` VALUES (749, 'izhlq423', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:23', '2025-09-11 20:41:45');
INSERT INTO `raffle_activity_account` VALUES (750, 'fextfv_hqj', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:23', '2025-09-11 20:41:46');
INSERT INTO `raffle_activity_account` VALUES (751, 'p1pvgw84', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:24', '2025-09-11 20:41:46');
INSERT INTO `raffle_activity_account` VALUES (752, 'iuk.ptd72', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:24', '2025-09-11 20:41:47');
INSERT INTO `raffle_activity_account` VALUES (753, 'jwui2v_n5e', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:24', '2025-09-11 20:41:48');
INSERT INTO `raffle_activity_account` VALUES (754, 'kutqrx_lal54', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:24', '2025-09-11 20:41:49');
INSERT INTO `raffle_activity_account` VALUES (755, 'p72k3d43', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:24', '2025-09-11 20:41:49');
INSERT INTO `raffle_activity_account` VALUES (756, 'rmftt4.k28', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:24', '2025-09-11 20:41:50');
INSERT INTO `raffle_activity_account` VALUES (757, 'i1cvdk31', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:24', '2025-09-11 20:41:51');
INSERT INTO `raffle_activity_account` VALUES (758, 'k8qscp97', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:24', '2025-09-11 20:41:52');
INSERT INTO `raffle_activity_account` VALUES (759, 'ixjlj1_kk17', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:25', '2025-09-11 20:41:53');
INSERT INTO `raffle_activity_account` VALUES (760, 'k5mmcx_ny262', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:25', '2025-09-11 20:41:53');
INSERT INTO `raffle_activity_account` VALUES (761, 'k18to3.lg016', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:25', '2025-09-11 20:41:54');
INSERT INTO `raffle_activity_account` VALUES (762, 'p72k3d.kjv', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:25', '2025-09-11 20:41:55');
INSERT INTO `raffle_activity_account` VALUES (763, 'o97scp33', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:25', '2025-09-11 20:41:56');
INSERT INTO `raffle_activity_account` VALUES (764, 'k5mit6.fk780', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:25', '2025-09-11 20:41:57');
INSERT INTO `raffle_activity_account` VALUES (765, 'k5q_io462', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:26', '2025-09-11 20:41:58');
INSERT INTO `raffle_activity_account` VALUES (766, 'gztmft22', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:26', '2025-09-11 20:41:58');
INSERT INTO `raffle_activity_account` VALUES (767, 'ixjit6_tti32', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:26', '2025-09-11 20:41:59');
INSERT INTO `raffle_activity_account` VALUES (768, 'fevfoz.kfz23', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:26', '2025-09-11 20:42:00');
INSERT INTO `raffle_activity_account` VALUES (769, 'oq3kgn_hge', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:26', '2025-09-11 20:42:01');
INSERT INTO `raffle_activity_account` VALUES (770, 'k7fv9r_fgw', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:26', '2025-09-11 20:42:02');
INSERT INTO `raffle_activity_account` VALUES (771, 'gs2m5656', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:27', '2025-09-11 20:42:02');
INSERT INTO `raffle_activity_account` VALUES (772, 'nlrlqi.p1k', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:27', '2025-09-11 20:42:03');
INSERT INTO `raffle_activity_account` VALUES (773, 'ucop8d_i4y87', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:27', '2025-09-11 20:42:04');
INSERT INTO `raffle_activity_account` VALUES (774, 'sf0ppm57', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:27', '2025-09-11 20:42:05');
INSERT INTO `raffle_activity_account` VALUES (775, 'ttbtw9_lr4', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:27', '2025-09-11 20:42:06');
INSERT INTO `raffle_activity_account` VALUES (776, 'k88to334', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:27', '2025-09-11 20:42:07');
INSERT INTO `raffle_activity_account` VALUES (777, 'ttbtw9.i3e86', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:27', '2025-09-11 20:42:07');
INSERT INTO `raffle_activity_account` VALUES (778, 'izhlq4.oti', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:28', '2025-09-11 20:42:08');
INSERT INTO `raffle_activity_account` VALUES (779, 'v9r61', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:28', '2025-09-11 20:42:09');
INSERT INTO `raffle_activity_account` VALUES (780, 'lm1sds_rkx', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:28', '2025-09-11 20:42:10');
INSERT INTO `raffle_activity_account` VALUES (781, 'ffxlmf_fk177', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:28', '2025-09-11 20:42:11');
INSERT INTO `raffle_activity_account` VALUES (782, 'knnk88.n5fi8w64', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:28', '2025-09-11 20:42:11');
INSERT INTO `raffle_activity_account` VALUES (783, 'tt4lj1.fze59', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:28', '2025-09-11 20:42:12');
INSERT INTO `raffle_activity_account` VALUES (784, 'k8qkfk.p9c', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:28', '2025-09-11 20:42:13');
INSERT INTO `raffle_activity_account` VALUES (785, 'ryilj139', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:28', '2025-09-11 20:42:14');
INSERT INTO `raffle_activity_account` VALUES (786, 'ttbtw9_gxn24', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:28', '2025-09-11 20:42:15');
INSERT INTO `raffle_activity_account` VALUES (787, 'v9rlnf_iwn40', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:28', '2025-09-11 20:42:15');
INSERT INTO `raffle_activity_account` VALUES (788, 'k5qkfk_tj383', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:28', '2025-09-11 20:42:16');
INSERT INTO `raffle_activity_account` VALUES (789, 'o97scp_i0k11', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:28', '2025-09-11 20:42:17');
INSERT INTO `raffle_activity_account` VALUES (790, 'k7znlr.s3r', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:28', '2025-09-11 20:42:18');
INSERT INTO `raffle_activity_account` VALUES (791, 'gs2m5680', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:29', '2025-09-11 20:42:19');
INSERT INTO `raffle_activity_account` VALUES (792, 'fmni4832', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:29', '2025-09-11 20:42:20');
INSERT INTO `raffle_activity_account` VALUES (793, 'o97scp87', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:29', '2025-09-11 20:42:20');
INSERT INTO `raffle_activity_account` VALUES (794, 'hm1i2v_sp2', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:29', '2025-09-11 20:42:21');
INSERT INTO `raffle_activity_account` VALUES (795, 'k5mmcx.jew', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:29', '2025-09-11 20:42:22');
INSERT INTO `raffle_activity_account` VALUES (796, 'tdcsbt.sbq1', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:29', '2025-09-11 20:42:22');
INSERT INTO `raffle_activity_account` VALUES (797, 'fh5q31_irk', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:29', '2025-09-11 20:42:23');
INSERT INTO `raffle_activity_account` VALUES (798, 'lh7lnf69', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:29', '2025-09-11 20:42:24');
INSERT INTO `raffle_activity_account` VALUES (799, 'fvpki7.ghc52', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:29', '2025-09-11 20:42:25');
INSERT INTO `raffle_activity_account` VALUES (800, 'tbfnuy40', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:30', '2025-09-11 20:42:25');
INSERT INTO `raffle_activity_account` VALUES (801, 'k5mmcx.n5fi8w', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:30', '2025-09-11 20:42:26');
INSERT INTO `raffle_activity_account` VALUES (802, 'gs2m56_tqo', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:30', '2025-09-11 20:42:27');
INSERT INTO `raffle_activity_account` VALUES (803, 'gs2k34_i3h8', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:30', '2025-09-11 20:42:28');
INSERT INTO `raffle_activity_account` VALUES (804, 'jcvsbt_rx7', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:30', '2025-09-11 20:42:29');
INSERT INTO `raffle_activity_account` VALUES (805, 'me4hce.nyg4', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:30', '2025-09-11 20:42:30');
INSERT INTO `raffle_activity_account` VALUES (806, 'mzitue.o2g87', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:30', '2025-09-11 20:42:30');
INSERT INTO `raffle_activity_account` VALUES (807, 'k88to3.tqj', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:31', '2025-09-11 20:42:31');
INSERT INTO `raffle_activity_account` VALUES (808, 'ui839', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:31', '2025-09-11 20:42:32');
INSERT INTO `raffle_activity_account` VALUES (809, 'lnf_ggu', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:31', '2025-09-11 20:42:33');
INSERT INTO `raffle_activity_account` VALUES (810, 'k8qkfk_ffg84', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:32', '2025-09-11 20:42:34');
INSERT INTO `raffle_activity_account` VALUES (811, 'k5qscp59', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:32', '2025-09-11 20:42:34');
INSERT INTO `raffle_activity_account` VALUES (812, 'lm1sbt6', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:32', '2025-09-11 20:42:35');
INSERT INTO `raffle_activity_account` VALUES (813, 'vacsbt97', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:32', '2025-09-11 20:42:36');
INSERT INTO `raffle_activity_account` VALUES (814, 'po4sf0_ib777', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:32', '2025-09-11 20:42:37');
INSERT INTO `raffle_activity_account` VALUES (815, 'lmfmhh.ire77', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:32', '2025-09-11 20:42:38');
INSERT INTO `raffle_activity_account` VALUES (816, 'ppm.nz1', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:32', '2025-09-11 20:42:38');
INSERT INTO `raffle_activity_account` VALUES (817, 'i1cvdk65', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:32', '2025-09-11 20:42:39');
INSERT INTO `raffle_activity_account` VALUES (818, 'k88to3_kfc', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:32', '2025-09-11 20:42:40');
INSERT INTO `raffle_activity_account` VALUES (819, 'sf0ppm.k2860', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:32', '2025-09-11 20:42:41');
INSERT INTO `raffle_activity_account` VALUES (820, 'ggqryd44', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:33', '2025-09-11 20:42:41');
INSERT INTO `raffle_activity_account` VALUES (821, 'pybsbt30', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:33', '2025-09-11 20:42:42');
INSERT INTO `raffle_activity_account` VALUES (822, 'tt6kfy.g4v78', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:33', '2025-09-11 20:42:43');
INSERT INTO `raffle_activity_account` VALUES (823, 'ucop8d.glk', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:33', '2025-09-11 20:42:44');
INSERT INTO `raffle_activity_account` VALUES (824, 'ryilj186', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:33', '2025-09-11 20:42:45');
INSERT INTO `raffle_activity_account` VALUES (825, 'fmnnzu.keb98', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:33', '2025-09-11 20:42:45');
INSERT INTO `raffle_activity_account` VALUES (826, 'o97fmn21', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:33', '2025-09-11 20:42:46');
INSERT INTO `raffle_activity_account` VALUES (827, 'k7ngvs_gbf', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:33', '2025-09-11 20:42:47');
INSERT INTO `raffle_activity_account` VALUES (828, 'ggqryd_i6hsf355', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:34', '2025-09-11 20:42:48');
INSERT INTO `raffle_activity_account` VALUES (829, 'i1cuji52', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:34', '2025-09-11 20:42:49');
INSERT INTO `raffle_activity_account` VALUES (830, 'pybsbt85', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:34', '2025-09-11 20:42:50');
INSERT INTO `raffle_activity_account` VALUES (831, 'lmfmhh_sl5', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:34', '2025-09-11 20:42:51');
INSERT INTO `raffle_activity_account` VALUES (832, 'ts5tbf24', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:34', '2025-09-11 20:42:51');
INSERT INTO `raffle_activity_account` VALUES (833, 'mzitue_sn143', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:35', '2025-09-11 20:42:52');
INSERT INTO `raffle_activity_account` VALUES (834, 'fevfoz.lpi22', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:35', '2025-09-11 20:42:53');
INSERT INTO `raffle_activity_account` VALUES (835, 'gs2m56_ggl72', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:35', '2025-09-11 20:42:54');
INSERT INTO `raffle_activity_account` VALUES (836, 'mzitue.tcu0', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:35', '2025-09-11 20:42:55');
INSERT INTO `raffle_activity_account` VALUES (837, 'izhsf0_go824', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:35', '2025-09-11 20:42:55');
INSERT INTO `raffle_activity_account` VALUES (838, 'negtue_psi98', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:35', '2025-09-11 20:42:56');
INSERT INTO `raffle_activity_account` VALUES (839, 'izhpd6_iwd81', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:35', '2025-09-11 20:42:57');
INSERT INTO `raffle_activity_account` VALUES (840, 'jcvsbt.fh869', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:36', '2025-09-11 20:42:58');
INSERT INTO `raffle_activity_account` VALUES (841, 'lzbmd2.fqn', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:36', '2025-09-11 20:42:59');
INSERT INTO `raffle_activity_account` VALUES (842, 'o97kgs_ghb18', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:36', '2025-09-11 20:42:59');
INSERT INTO `raffle_activity_account` VALUES (843, 'g6nmzi_irk', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:36', '2025-09-11 20:43:00');
INSERT INTO `raffle_activity_account` VALUES (844, 'mawnuy_hce', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:36', '2025-09-11 20:43:01');
INSERT INTO `raffle_activity_account` VALUES (845, 'pd6fvp_ssq', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:36', '2025-09-11 20:43:02');
INSERT INTO `raffle_activity_account` VALUES (846, 'tw6my276', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:36', '2025-09-11 20:43:03');
INSERT INTO `raffle_activity_account` VALUES (847, 'mzitue_nj340', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:37', '2025-09-11 20:43:03');
INSERT INTO `raffle_activity_account` VALUES (848, 'pa8kfk6', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:37', '2025-09-11 20:43:04');
INSERT INTO `raffle_activity_account` VALUES (849, 'k8qi48_tml1', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:37', '2025-09-11 20:43:05');
INSERT INTO `raffle_activity_account` VALUES (850, 'm8eiuk_g3f', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:37', '2025-09-11 20:43:06');
INSERT INTO `raffle_activity_account` VALUES (851, 'ktzmzi_scp', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:37', '2025-09-11 20:43:07');
INSERT INTO `raffle_activity_account` VALUES (852, 'n0emzg_tn27', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:37', '2025-09-11 20:43:07');
INSERT INTO `raffle_activity_account` VALUES (853, 'k1zsbt_l9x91', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:37', '2025-09-11 20:43:08');
INSERT INTO `raffle_activity_account` VALUES (854, 'lmfmhh_kd7', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:38', '2025-09-11 20:43:09');
INSERT INTO `raffle_activity_account` VALUES (855, 'k5qsbt.oar93', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:38', '2025-09-11 20:43:10');
INSERT INTO `raffle_activity_account` VALUES (856, 'k18iui_tn253', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:38', '2025-09-11 20:43:11');
INSERT INTO `raffle_activity_account` VALUES (857, 'k18iui_ist', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:38', '2025-09-11 20:43:11');
INSERT INTO `raffle_activity_account` VALUES (858, 'rydkjv_qj262', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:38', '2025-09-11 20:43:12');
INSERT INTO `raffle_activity_account` VALUES (859, 'i1cl5v.ioq82', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:38', '2025-09-11 20:43:13');
INSERT INTO `raffle_activity_account` VALUES (860, 'v9rmdm75', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:38', '2025-09-11 20:43:14');
INSERT INTO `raffle_activity_account` VALUES (861, 'hm1nuy_tt6', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:38', '2025-09-11 20:43:15');
INSERT INTO `raffle_activity_account` VALUES (862, 'ggqk1z.fj980', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:38', '2025-09-11 20:43:15');
INSERT INTO `raffle_activity_account` VALUES (863, 'ryilj19', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:39', '2025-09-11 20:43:16');
INSERT INTO `raffle_activity_account` VALUES (864, 'ppm_sks30', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:39', '2025-09-11 20:43:17');
INSERT INTO `raffle_activity_account` VALUES (865, 'hm1i2v26', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:39', '2025-09-11 20:43:18');
INSERT INTO `raffle_activity_account` VALUES (866, 'ffxlmf_lrs54', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:39', '2025-09-11 20:43:19');
INSERT INTO `raffle_activity_account` VALUES (867, 'ggqk1z31', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:39', '2025-09-11 20:43:20');
INSERT INTO `raffle_activity_account` VALUES (868, 'ts5hvb_ssq', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:39', '2025-09-11 20:43:20');
INSERT INTO `raffle_activity_account` VALUES (869, 'lh7lnf.g53', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:40', '2025-09-11 20:43:21');
INSERT INTO `raffle_activity_account` VALUES (870, 'lh7k1z_i46', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:40', '2025-09-11 20:43:22');
INSERT INTO `raffle_activity_account` VALUES (871, 'mawfmn57', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:40', '2025-09-11 20:43:23');
INSERT INTO `raffle_activity_account` VALUES (872, 'pybsbt.fnq', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:40', '2025-09-11 20:43:24');
INSERT INTO `raffle_activity_account` VALUES (873, 'i8lv9r.jdb', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:40', '2025-09-11 20:43:25');
INSERT INTO `raffle_activity_account` VALUES (874, 'gs2k34_nez', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:40', '2025-09-11 20:43:26');
INSERT INTO `raffle_activity_account` VALUES (875, 'ts5hvb.q7z14', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:40', '2025-09-11 20:43:26');
INSERT INTO `raffle_activity_account` VALUES (876, 'tw9izl_puf', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:40', '2025-09-11 20:43:27');
INSERT INTO `raffle_activity_account` VALUES (877, 'i1cvdk.sk766', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:41', '2025-09-11 20:43:28');
INSERT INTO `raffle_activity_account` VALUES (878, 'is8k1z_fgs', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:41', '2025-09-11 20:43:29');
INSERT INTO `raffle_activity_account` VALUES (879, 'jcvsbt87', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:41', '2025-09-11 20:43:29');
INSERT INTO `raffle_activity_account` VALUES (880, 'gs2k34_u4f88', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:42', '2025-09-11 20:43:30');
INSERT INTO `raffle_activity_account` VALUES (881, 'k5mmcx74', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:42', '2025-09-11 20:43:31');
INSERT INTO `raffle_activity_account` VALUES (882, 'k5mmcx.qg973', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:42', '2025-09-11 20:43:32');
INSERT INTO `raffle_activity_account` VALUES (883, 'oq3kgn.gl4o6y10', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:42', '2025-09-11 20:43:33');
INSERT INTO `raffle_activity_account` VALUES (884, 'k7ngvs.gky42', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:42', '2025-09-11 20:43:33');
INSERT INTO `raffle_activity_account` VALUES (885, 'ffxlmf_gqg33', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:42', '2025-09-11 20:43:34');
INSERT INTO `raffle_activity_account` VALUES (886, 'fvpts4_qg435', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:43', '2025-09-11 20:43:35');
INSERT INTO `raffle_activity_account` VALUES (887, 'lm1i2v63', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:43', '2025-09-11 20:43:36');
INSERT INTO `raffle_activity_account` VALUES (888, 'i1clo5_oti', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:43', '2025-09-11 20:43:37');
INSERT INTO `raffle_activity_account` VALUES (889, 'oq3kgn39', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:43', '2025-09-11 20:43:37');
INSERT INTO `raffle_activity_account` VALUES (890, 'fmnnzu.sib', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:43', '2025-09-11 20:43:38');
INSERT INTO `raffle_activity_account` VALUES (891, 'k18iui23', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:43', '2025-09-11 20:43:39');
INSERT INTO `raffle_activity_account` VALUES (892, 'hm1nuy_lid', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:43', '2025-09-11 20:43:40');
INSERT INTO `raffle_activity_account` VALUES (893, 'jlbffg18', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:43', '2025-09-11 20:43:41');
INSERT INTO `raffle_activity_account` VALUES (894, 'pgknz911', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:43', '2025-09-11 20:43:41');
INSERT INTO `raffle_activity_account` VALUES (895, 'k1zu0c_skj34', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:44', '2025-09-11 20:43:42');
INSERT INTO `raffle_activity_account` VALUES (896, 'jlbffg20', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:44', '2025-09-11 20:43:43');
INSERT INTO `raffle_activity_account` VALUES (897, 'vacsbt86', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:44', '2025-09-11 20:43:44');
INSERT INTO `raffle_activity_account` VALUES (898, 'v9bmdm.s3r72', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:44', '2025-09-11 20:43:44');
INSERT INTO `raffle_activity_account` VALUES (899, 'jzzgm3_ntz', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:44', '2025-09-11 20:43:45');
INSERT INTO `raffle_activity_account` VALUES (900, 'k8qlqi.nel95', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:44', '2025-09-11 20:43:46');
INSERT INTO `raffle_activity_account` VALUES (901, 'gztmft.i85', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:45', '2025-09-11 20:43:47');
INSERT INTO `raffle_activity_account` VALUES (902, 'izhlu8_kgs62', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:45', '2025-09-11 20:43:48');
INSERT INTO `raffle_activity_account` VALUES (903, 'sf0ppm_onw', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:45', '2025-09-11 20:43:48');
INSERT INTO `raffle_activity_account` VALUES (904, 'u6mgge_sfi', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:45', '2025-09-11 20:43:49');
INSERT INTO `raffle_activity_account` VALUES (905, 'hm1hs7_fep', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:45', '2025-09-11 20:43:50');
INSERT INTO `raffle_activity_account` VALUES (906, 'mzitue_lzw', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:45', '2025-09-11 20:43:51');
INSERT INTO `raffle_activity_account` VALUES (907, 'fgwuih.gkv94', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:45', '2025-09-11 20:43:52');
INSERT INTO `raffle_activity_account` VALUES (908, 'ttbtw9_hlm32', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:46', '2025-09-11 20:43:53');
INSERT INTO `raffle_activity_account` VALUES (909, 'me4nzu15', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:46', '2025-09-11 20:43:53');
INSERT INTO `raffle_activity_account` VALUES (910, 'lm1_tti', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:46', '2025-09-11 20:43:54');

-- ----------------------------
-- Table structure for raffle_activity_account_day
-- ----------------------------
DROP TABLE IF EXISTS `raffle_activity_account_day`;
CREATE TABLE `raffle_activity_account_day`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `day` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '日期（yyyy-mm-dd）',
  `day_count` int NOT NULL COMMENT '日次数',
  `day_count_surplus` int NOT NULL COMMENT '日次数-剩余',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_user_id_activity_id_day`(`user_id` ASC, `activity_id` ASC, `day` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 281 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抽奖活动账户表-日次数' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of raffle_activity_account_day
-- ----------------------------
INSERT INTO `raffle_activity_account_day` VALUES (18, 'Jay', 100301, '2025-09-11', 1200, 1093, '2025-09-11 15:35:41', '2025-09-11 20:40:32');
INSERT INTO `raffle_activity_account_day` VALUES (19, 'fvpki7.keb22', 100301, '2025-09-11', 1000, 992, '2025-09-11 20:22:02', '2025-09-11 20:40:32');
INSERT INTO `raffle_activity_account_day` VALUES (20, 'ixjlj1_fgs', 100301, '2025-09-11', 1000, 993, '2025-09-11 20:37:05', '2025-09-11 20:40:33');
INSERT INTO `raffle_activity_account_day` VALUES (21, 'v9bmdm98', 100301, '2025-09-11', 1000, 997, '2025-09-11 20:37:06', '2025-09-11 20:40:34');
INSERT INTO `raffle_activity_account_day` VALUES (22, 'l7nrqi39', 100301, '2025-09-11', 1000, 998, '2025-09-11 20:37:06', '2025-09-11 20:40:35');
INSERT INTO `raffle_activity_account_day` VALUES (23, 'tbfv9r_l7q84', 100301, '2025-09-11', 1000, 998, '2025-09-11 20:37:07', '2025-09-11 20:40:35');
INSERT INTO `raffle_activity_account_day` VALUES (24, 'jwupu5.gjv', 100301, '2025-09-11', 1000, 998, '2025-09-11 20:37:08', '2025-09-11 20:40:36');
INSERT INTO `raffle_activity_account_day` VALUES (25, 'lh7lnf.iul17', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:37', '2025-09-11 20:40:37');
INSERT INTO `raffle_activity_account_day` VALUES (26, 'p72k3d55', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:37', '2025-09-11 20:40:37');
INSERT INTO `raffle_activity_account_day` VALUES (27, 'izhfoz52', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:38', '2025-09-11 20:40:38');
INSERT INTO `raffle_activity_account_day` VALUES (28, 'jlbi46_jew', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:39', '2025-09-11 20:40:39');
INSERT INTO `raffle_activity_account_day` VALUES (29, 'vdak5x51', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:39', '2025-09-11 20:40:39');
INSERT INTO `raffle_activity_account_day` VALUES (30, 'k1zsbt76', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:40', '2025-09-11 20:40:40');
INSERT INTO `raffle_activity_account_day` VALUES (31, 'v9blnf12', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:41', '2025-09-11 20:40:41');
INSERT INTO `raffle_activity_account_day` VALUES (32, 'ggqryd_skd44', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:42', '2025-09-11 20:40:42');
INSERT INTO `raffle_activity_account_day` VALUES (33, 'fsekfk.jh485', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:42', '2025-09-11 20:40:42');
INSERT INTO `raffle_activity_account_day` VALUES (34, 'k8qkfk86', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:43', '2025-09-11 20:40:43');
INSERT INTO `raffle_activity_account_day` VALUES (35, 'pd6fvp_kd762', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:44', '2025-09-11 20:40:44');
INSERT INTO `raffle_activity_account_day` VALUES (36, 'irescp24', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:44', '2025-09-11 20:40:44');
INSERT INTO `raffle_activity_account_day` VALUES (37, 'lm1_p1k', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:45', '2025-09-11 20:40:45');
INSERT INTO `raffle_activity_account_day` VALUES (38, 'tgmo3v_tg9', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:46', '2025-09-11 20:40:46');
INSERT INTO `raffle_activity_account_day` VALUES (39, 'sf0ppm47', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:46', '2025-09-11 20:40:46');
INSERT INTO `raffle_activity_account_day` VALUES (40, 'pgknz9.q5795', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:47', '2025-09-11 20:40:47');
INSERT INTO `raffle_activity_account_day` VALUES (41, 'gs2m5650', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:48', '2025-09-11 20:40:48');
INSERT INTO `raffle_activity_account_day` VALUES (42, 'jzzgm341', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:48', '2025-09-11 20:40:48');
INSERT INTO `raffle_activity_account_day` VALUES (43, 'k8qscp_glx72', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:49', '2025-09-11 20:40:49');
INSERT INTO `raffle_activity_account_day` VALUES (44, 'i2xmy224', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:50', '2025-09-11 20:40:50');
INSERT INTO `raffle_activity_account_day` VALUES (45, 'nlrlqi_j7y', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:50', '2025-09-11 20:40:50');
INSERT INTO `raffle_activity_account_day` VALUES (46, 'ryilj149', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:51', '2025-09-11 20:40:51');
INSERT INTO `raffle_activity_account_day` VALUES (47, 'mzitue_ol733', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:52', '2025-09-11 20:40:52');
INSERT INTO `raffle_activity_account_day` VALUES (48, 'ixjlj1_tqo', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:52', '2025-09-11 20:40:52');
INSERT INTO `raffle_activity_account_day` VALUES (49, 'lm1i2v83', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:53', '2025-09-11 20:40:53');
INSERT INTO `raffle_activity_account_day` VALUES (50, 'ixjit6.laj61', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:54', '2025-09-11 20:40:54');
INSERT INTO `raffle_activity_account_day` VALUES (51, 'm97k5q.ny968', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:54', '2025-09-11 20:40:54');
INSERT INTO `raffle_activity_account_day` VALUES (52, 'ui8_n1l68', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:55', '2025-09-11 20:40:55');
INSERT INTO `raffle_activity_account_day` VALUES (53, 'tt6kfy43', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:56', '2025-09-11 20:40:56');
INSERT INTO `raffle_activity_account_day` VALUES (54, 'k1z74', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:56', '2025-09-11 20:40:56');
INSERT INTO `raffle_activity_account_day` VALUES (55, 'oq3kgn38', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:57', '2025-09-11 20:40:57');
INSERT INTO `raffle_activity_account_day` VALUES (56, 'kgs_man44', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:58', '2025-09-11 20:40:58');
INSERT INTO `raffle_activity_account_day` VALUES (57, 'i1csbt_gqo91', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:58', '2025-09-11 20:40:58');
INSERT INTO `raffle_activity_account_day` VALUES (58, 'ts5hvb.skx', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:40:59', '2025-09-11 20:40:59');
INSERT INTO `raffle_activity_account_day` VALUES (59, 'm8eiuk_fl0', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:00', '2025-09-11 20:41:00');
INSERT INTO `raffle_activity_account_day` VALUES (60, 'k8qkfk.sks1', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:00', '2025-09-11 20:41:00');
INSERT INTO `raffle_activity_account_day` VALUES (61, 'ui865', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:01', '2025-09-11 20:41:01');
INSERT INTO `raffle_activity_account_day` VALUES (62, 'i2xmy2_fj6', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:02', '2025-09-11 20:41:02');
INSERT INTO `raffle_activity_account_day` VALUES (63, 'izhpd6_jwt19', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:03', '2025-09-11 20:41:03');
INSERT INTO `raffle_activity_account_day` VALUES (64, 'tw6my286', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:03', '2025-09-11 20:41:03');
INSERT INTO `raffle_activity_account_day` VALUES (65, 'pa8kfk35', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:04', '2025-09-11 20:41:04');
INSERT INTO `raffle_activity_account_day` VALUES (66, 'tt6kfy.i809', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:05', '2025-09-11 20:41:05');
INSERT INTO `raffle_activity_account_day` VALUES (67, 'i1cvdk88', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:05', '2025-09-11 20:41:05');
INSERT INTO `raffle_activity_account_day` VALUES (68, 'mawfmn82', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:06', '2025-09-11 20:41:06');
INSERT INTO `raffle_activity_account_day` VALUES (69, 'fmnrmi_ff1', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:07', '2025-09-11 20:41:07');
INSERT INTO `raffle_activity_account_day` VALUES (70, 'me4nzu_ph932', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:07', '2025-09-11 20:41:07');
INSERT INTO `raffle_activity_account_day` VALUES (71, 'tbfv9r_ssq', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:08', '2025-09-11 20:41:08');
INSERT INTO `raffle_activity_account_day` VALUES (72, 'v9rlnf.gbl49', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:09', '2025-09-11 20:41:09');
INSERT INTO `raffle_activity_account_day` VALUES (73, 'k5mhm1_ky2', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:09', '2025-09-11 20:41:09');
INSERT INTO `raffle_activity_account_day` VALUES (74, 'k5qkfk.qap8', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:10', '2025-09-11 20:41:10');
INSERT INTO `raffle_activity_account_day` VALUES (75, 'pu9ggq.gc3', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:11', '2025-09-11 20:41:11');
INSERT INTO `raffle_activity_account_day` VALUES (76, 'rmftt492', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:11', '2025-09-11 20:41:11');
INSERT INTO `raffle_activity_account_day` VALUES (77, 'tt6kfy.lly', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:12', '2025-09-11 20:41:12');
INSERT INTO `raffle_activity_account_day` VALUES (78, 'ui859', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:13', '2025-09-11 20:41:13');
INSERT INTO `raffle_activity_account_day` VALUES (79, 'v9ru724', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:13', '2025-09-11 20:41:13');
INSERT INTO `raffle_activity_account_day` VALUES (80, 'hm1nuy43', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:14', '2025-09-11 20:41:14');
INSERT INTO `raffle_activity_account_day` VALUES (81, 'nlrlqi66', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:15', '2025-09-11 20:41:15');
INSERT INTO `raffle_activity_account_day` VALUES (82, 'fevfoz.gne23', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:16', '2025-09-11 20:41:16');
INSERT INTO `raffle_activity_account_day` VALUES (83, 'k8qi48.pip39', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:17', '2025-09-11 20:41:17');
INSERT INTO `raffle_activity_account_day` VALUES (84, 'mzitue.m2hgkw', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:17', '2025-09-11 20:41:17');
INSERT INTO `raffle_activity_account_day` VALUES (85, 'izhlq4.lpvfim58', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:18', '2025-09-11 20:41:18');
INSERT INTO `raffle_activity_account_day` VALUES (86, 'lh7lnf96', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:19', '2025-09-11 20:41:19');
INSERT INTO `raffle_activity_account_day` VALUES (87, 'fh5q31.ljf', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:20', '2025-09-11 20:41:20');
INSERT INTO `raffle_activity_account_day` VALUES (88, 'gs2k34_ny930', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:21', '2025-09-11 20:41:21');
INSERT INTO `raffle_activity_account_day` VALUES (89, 'mawtue37', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:21', '2025-09-11 20:41:21');
INSERT INTO `raffle_activity_account_day` VALUES (90, 'jwupu524', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:22', '2025-09-11 20:41:22');
INSERT INTO `raffle_activity_account_day` VALUES (91, 'pu9ggq_h5q81', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:23', '2025-09-11 20:41:23');
INSERT INTO `raffle_activity_account_day` VALUES (92, 'hm1nuy_frz89', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:24', '2025-09-11 20:41:24');
INSERT INTO `raffle_activity_account_day` VALUES (93, 'tgmo3v.tfa99', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:25', '2025-09-11 20:41:25');
INSERT INTO `raffle_activity_account_day` VALUES (94, 'k1zu0c.gbf58', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:26', '2025-09-11 20:41:26');
INSERT INTO `raffle_activity_account_day` VALUES (95, 'id7mft99', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:26', '2025-09-11 20:41:26');
INSERT INTO `raffle_activity_account_day` VALUES (96, 'tbfnuy58', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:27', '2025-09-11 20:41:27');
INSERT INTO `raffle_activity_account_day` VALUES (97, 'ffxlmf_l9v84', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:28', '2025-09-11 20:41:28');
INSERT INTO `raffle_activity_account_day` VALUES (98, 'fmnrmi_ptd', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:29', '2025-09-11 20:41:29');
INSERT INTO `raffle_activity_account_day` VALUES (99, 'tt4lj1_vix54', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:30', '2025-09-11 20:41:30');
INSERT INTO `raffle_activity_account_day` VALUES (100, 'irescp12', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:30', '2025-09-11 20:41:30');
INSERT INTO `raffle_activity_account_day` VALUES (101, 'ttbtw992', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:31', '2025-09-11 20:41:31');
INSERT INTO `raffle_activity_account_day` VALUES (102, 'fozmy264', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:32', '2025-09-11 20:41:32');
INSERT INTO `raffle_activity_account_day` VALUES (103, 'rydkjv.g3k46', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:33', '2025-09-11 20:41:33');
INSERT INTO `raffle_activity_account_day` VALUES (104, 'fgwuih10', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:33', '2025-09-11 20:41:33');
INSERT INTO `raffle_activity_account_day` VALUES (105, 'gs2m5634', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:34', '2025-09-11 20:41:34');
INSERT INTO `raffle_activity_account_day` VALUES (106, 'mawnuy3', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:35', '2025-09-11 20:41:35');
INSERT INTO `raffle_activity_account_day` VALUES (107, 'mawfmn_rlk', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:36', '2025-09-11 20:41:36');
INSERT INTO `raffle_activity_account_day` VALUES (108, 'izhfoz96', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:37', '2025-09-11 20:41:37');
INSERT INTO `raffle_activity_account_day` VALUES (109, 'p72k3d.rvz', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:37', '2025-09-11 20:41:37');
INSERT INTO `raffle_activity_account_day` VALUES (110, 'izhpd6_lxi', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:38', '2025-09-11 20:41:38');
INSERT INTO `raffle_activity_account_day` VALUES (111, 'jzzgm374', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:39', '2025-09-11 20:41:39');
INSERT INTO `raffle_activity_account_day` VALUES (112, 'i1cuji.nev32', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:40', '2025-09-11 20:41:40');
INSERT INTO `raffle_activity_account_day` VALUES (113, 'lj1ljf_g8b10', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:41', '2025-09-11 20:41:41');
INSERT INTO `raffle_activity_account_day` VALUES (114, 'hm1nuy.lqd', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:42', '2025-09-11 20:41:42');
INSERT INTO `raffle_activity_account_day` VALUES (115, 'k1zggq.o34', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:42', '2025-09-11 20:41:42');
INSERT INTO `raffle_activity_account_day` VALUES (116, 'v9rmdm_gp82', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:43', '2025-09-11 20:41:43');
INSERT INTO `raffle_activity_account_day` VALUES (117, 'o97scp88', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:44', '2025-09-11 20:41:44');
INSERT INTO `raffle_activity_account_day` VALUES (118, 'izhlq423', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:45', '2025-09-11 20:41:45');
INSERT INTO `raffle_activity_account_day` VALUES (119, 'fextfv_hqj', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:46', '2025-09-11 20:41:46');
INSERT INTO `raffle_activity_account_day` VALUES (120, 'p1pvgw84', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:46', '2025-09-11 20:41:46');
INSERT INTO `raffle_activity_account_day` VALUES (121, 'iuk.ptd72', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:47', '2025-09-11 20:41:47');
INSERT INTO `raffle_activity_account_day` VALUES (122, 'jwui2v_n5e', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:48', '2025-09-11 20:41:48');
INSERT INTO `raffle_activity_account_day` VALUES (123, 'kutqrx_lal54', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:49', '2025-09-11 20:41:49');
INSERT INTO `raffle_activity_account_day` VALUES (124, 'p72k3d43', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:49', '2025-09-11 20:41:49');
INSERT INTO `raffle_activity_account_day` VALUES (125, 'rmftt4.k28', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:50', '2025-09-11 20:41:50');
INSERT INTO `raffle_activity_account_day` VALUES (126, 'i1cvdk31', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:51', '2025-09-11 20:41:51');
INSERT INTO `raffle_activity_account_day` VALUES (127, 'k8qscp97', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:52', '2025-09-11 20:41:52');
INSERT INTO `raffle_activity_account_day` VALUES (128, 'ixjlj1_kk17', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:53', '2025-09-11 20:41:53');
INSERT INTO `raffle_activity_account_day` VALUES (129, 'k5mmcx_ny262', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:53', '2025-09-11 20:41:53');
INSERT INTO `raffle_activity_account_day` VALUES (130, 'k18to3.lg016', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:54', '2025-09-11 20:41:54');
INSERT INTO `raffle_activity_account_day` VALUES (131, 'p72k3d.kjv', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:55', '2025-09-11 20:41:55');
INSERT INTO `raffle_activity_account_day` VALUES (132, 'o97scp33', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:56', '2025-09-11 20:41:56');
INSERT INTO `raffle_activity_account_day` VALUES (133, 'k5mit6.fk780', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:57', '2025-09-11 20:41:57');
INSERT INTO `raffle_activity_account_day` VALUES (134, 'k5q_io462', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:58', '2025-09-11 20:41:58');
INSERT INTO `raffle_activity_account_day` VALUES (135, 'gztmft22', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:58', '2025-09-11 20:41:58');
INSERT INTO `raffle_activity_account_day` VALUES (136, 'ixjit6_tti32', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:41:59', '2025-09-11 20:41:59');
INSERT INTO `raffle_activity_account_day` VALUES (137, 'fevfoz.kfz23', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:00', '2025-09-11 20:42:00');
INSERT INTO `raffle_activity_account_day` VALUES (138, 'oq3kgn_hge', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:01', '2025-09-11 20:42:01');
INSERT INTO `raffle_activity_account_day` VALUES (139, 'k7fv9r_fgw', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:02', '2025-09-11 20:42:02');
INSERT INTO `raffle_activity_account_day` VALUES (140, 'gs2m5656', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:02', '2025-09-11 20:42:02');
INSERT INTO `raffle_activity_account_day` VALUES (141, 'nlrlqi.p1k', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:03', '2025-09-11 20:42:03');
INSERT INTO `raffle_activity_account_day` VALUES (142, 'ucop8d_i4y87', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:04', '2025-09-11 20:42:04');
INSERT INTO `raffle_activity_account_day` VALUES (143, 'sf0ppm57', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:05', '2025-09-11 20:42:05');
INSERT INTO `raffle_activity_account_day` VALUES (144, 'ttbtw9_lr4', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:06', '2025-09-11 20:42:06');
INSERT INTO `raffle_activity_account_day` VALUES (145, 'k88to334', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:07', '2025-09-11 20:42:07');
INSERT INTO `raffle_activity_account_day` VALUES (146, 'ttbtw9.i3e86', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:07', '2025-09-11 20:42:07');
INSERT INTO `raffle_activity_account_day` VALUES (147, 'izhlq4.oti', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:08', '2025-09-11 20:42:08');
INSERT INTO `raffle_activity_account_day` VALUES (148, 'v9r61', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:09', '2025-09-11 20:42:09');
INSERT INTO `raffle_activity_account_day` VALUES (149, 'lm1sds_rkx', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:10', '2025-09-11 20:42:10');
INSERT INTO `raffle_activity_account_day` VALUES (150, 'ffxlmf_fk177', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:11', '2025-09-11 20:42:11');
INSERT INTO `raffle_activity_account_day` VALUES (151, 'knnk88.n5fi8w64', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:11', '2025-09-11 20:42:11');
INSERT INTO `raffle_activity_account_day` VALUES (152, 'tt4lj1.fze59', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:12', '2025-09-11 20:42:12');
INSERT INTO `raffle_activity_account_day` VALUES (153, 'k8qkfk.p9c', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:13', '2025-09-11 20:42:13');
INSERT INTO `raffle_activity_account_day` VALUES (154, 'ryilj139', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:14', '2025-09-11 20:42:14');
INSERT INTO `raffle_activity_account_day` VALUES (155, 'ttbtw9_gxn24', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:15', '2025-09-11 20:42:15');
INSERT INTO `raffle_activity_account_day` VALUES (156, 'v9rlnf_iwn40', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:15', '2025-09-11 20:42:15');
INSERT INTO `raffle_activity_account_day` VALUES (157, 'k5qkfk_tj383', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:16', '2025-09-11 20:42:16');
INSERT INTO `raffle_activity_account_day` VALUES (158, 'o97scp_i0k11', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:17', '2025-09-11 20:42:17');
INSERT INTO `raffle_activity_account_day` VALUES (159, 'k7znlr.s3r', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:18', '2025-09-11 20:42:18');
INSERT INTO `raffle_activity_account_day` VALUES (160, 'gs2m5680', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:19', '2025-09-11 20:42:19');
INSERT INTO `raffle_activity_account_day` VALUES (161, 'fmni4832', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:20', '2025-09-11 20:42:20');
INSERT INTO `raffle_activity_account_day` VALUES (162, 'o97scp87', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:20', '2025-09-11 20:42:20');
INSERT INTO `raffle_activity_account_day` VALUES (163, 'hm1i2v_sp2', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:21', '2025-09-11 20:42:21');
INSERT INTO `raffle_activity_account_day` VALUES (164, 'k5mmcx.jew', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:22', '2025-09-11 20:42:22');
INSERT INTO `raffle_activity_account_day` VALUES (165, 'tdcsbt.sbq1', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:22', '2025-09-11 20:42:22');
INSERT INTO `raffle_activity_account_day` VALUES (166, 'fh5q31_irk', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:23', '2025-09-11 20:42:23');
INSERT INTO `raffle_activity_account_day` VALUES (167, 'lh7lnf69', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:24', '2025-09-11 20:42:24');
INSERT INTO `raffle_activity_account_day` VALUES (168, 'fvpki7.ghc52', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:25', '2025-09-11 20:42:25');
INSERT INTO `raffle_activity_account_day` VALUES (169, 'tbfnuy40', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:25', '2025-09-11 20:42:25');
INSERT INTO `raffle_activity_account_day` VALUES (170, 'k5mmcx.n5fi8w', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:26', '2025-09-11 20:42:26');
INSERT INTO `raffle_activity_account_day` VALUES (171, 'gs2m56_tqo', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:27', '2025-09-11 20:42:27');
INSERT INTO `raffle_activity_account_day` VALUES (172, 'gs2k34_i3h8', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:28', '2025-09-11 20:42:28');
INSERT INTO `raffle_activity_account_day` VALUES (173, 'jcvsbt_rx7', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:29', '2025-09-11 20:42:29');
INSERT INTO `raffle_activity_account_day` VALUES (174, 'me4hce.nyg4', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:30', '2025-09-11 20:42:30');
INSERT INTO `raffle_activity_account_day` VALUES (175, 'mzitue.o2g87', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:30', '2025-09-11 20:42:30');
INSERT INTO `raffle_activity_account_day` VALUES (176, 'k88to3.tqj', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:31', '2025-09-11 20:42:31');
INSERT INTO `raffle_activity_account_day` VALUES (177, 'ui839', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:32', '2025-09-11 20:42:32');
INSERT INTO `raffle_activity_account_day` VALUES (178, 'lnf_ggu', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:33', '2025-09-11 20:42:33');
INSERT INTO `raffle_activity_account_day` VALUES (179, 'k8qkfk_ffg84', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:34', '2025-09-11 20:42:34');
INSERT INTO `raffle_activity_account_day` VALUES (180, 'k5qscp59', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:34', '2025-09-11 20:42:34');
INSERT INTO `raffle_activity_account_day` VALUES (181, 'lm1sbt6', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:35', '2025-09-11 20:42:35');
INSERT INTO `raffle_activity_account_day` VALUES (182, 'vacsbt97', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:36', '2025-09-11 20:42:36');
INSERT INTO `raffle_activity_account_day` VALUES (183, 'po4sf0_ib777', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:37', '2025-09-11 20:42:37');
INSERT INTO `raffle_activity_account_day` VALUES (184, 'lmfmhh.ire77', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:38', '2025-09-11 20:42:38');
INSERT INTO `raffle_activity_account_day` VALUES (185, 'ppm.nz1', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:38', '2025-09-11 20:42:38');
INSERT INTO `raffle_activity_account_day` VALUES (186, 'i1cvdk65', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:39', '2025-09-11 20:42:39');
INSERT INTO `raffle_activity_account_day` VALUES (187, 'k88to3_kfc', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:40', '2025-09-11 20:42:40');
INSERT INTO `raffle_activity_account_day` VALUES (188, 'sf0ppm.k2860', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:41', '2025-09-11 20:42:41');
INSERT INTO `raffle_activity_account_day` VALUES (189, 'ggqryd44', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:41', '2025-09-11 20:42:41');
INSERT INTO `raffle_activity_account_day` VALUES (190, 'pybsbt30', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:42', '2025-09-11 20:42:42');
INSERT INTO `raffle_activity_account_day` VALUES (191, 'tt6kfy.g4v78', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:43', '2025-09-11 20:42:43');
INSERT INTO `raffle_activity_account_day` VALUES (192, 'ucop8d.glk', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:44', '2025-09-11 20:42:44');
INSERT INTO `raffle_activity_account_day` VALUES (193, 'ryilj186', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:45', '2025-09-11 20:42:45');
INSERT INTO `raffle_activity_account_day` VALUES (194, 'fmnnzu.keb98', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:45', '2025-09-11 20:42:45');
INSERT INTO `raffle_activity_account_day` VALUES (195, 'o97fmn21', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:46', '2025-09-11 20:42:46');
INSERT INTO `raffle_activity_account_day` VALUES (196, 'k7ngvs_gbf', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:47', '2025-09-11 20:42:47');
INSERT INTO `raffle_activity_account_day` VALUES (197, 'ggqryd_i6hsf355', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:48', '2025-09-11 20:42:48');
INSERT INTO `raffle_activity_account_day` VALUES (198, 'i1cuji52', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:49', '2025-09-11 20:42:49');
INSERT INTO `raffle_activity_account_day` VALUES (199, 'pybsbt85', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:50', '2025-09-11 20:42:50');
INSERT INTO `raffle_activity_account_day` VALUES (200, 'lmfmhh_sl5', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:51', '2025-09-11 20:42:51');
INSERT INTO `raffle_activity_account_day` VALUES (201, 'ts5tbf24', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:51', '2025-09-11 20:42:51');
INSERT INTO `raffle_activity_account_day` VALUES (202, 'mzitue_sn143', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:52', '2025-09-11 20:42:52');
INSERT INTO `raffle_activity_account_day` VALUES (203, 'fevfoz.lpi22', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:53', '2025-09-11 20:42:53');
INSERT INTO `raffle_activity_account_day` VALUES (204, 'gs2m56_ggl72', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:54', '2025-09-11 20:42:54');
INSERT INTO `raffle_activity_account_day` VALUES (205, 'mzitue.tcu0', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:55', '2025-09-11 20:42:55');
INSERT INTO `raffle_activity_account_day` VALUES (206, 'izhsf0_go824', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:55', '2025-09-11 20:42:55');
INSERT INTO `raffle_activity_account_day` VALUES (207, 'negtue_psi98', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:56', '2025-09-11 20:42:56');
INSERT INTO `raffle_activity_account_day` VALUES (208, 'izhpd6_iwd81', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:57', '2025-09-11 20:42:57');
INSERT INTO `raffle_activity_account_day` VALUES (209, 'jcvsbt.fh869', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:58', '2025-09-11 20:42:58');
INSERT INTO `raffle_activity_account_day` VALUES (210, 'lzbmd2.fqn', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:59', '2025-09-11 20:42:59');
INSERT INTO `raffle_activity_account_day` VALUES (211, 'o97kgs_ghb18', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:42:59', '2025-09-11 20:42:59');
INSERT INTO `raffle_activity_account_day` VALUES (212, 'g6nmzi_irk', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:00', '2025-09-11 20:43:00');
INSERT INTO `raffle_activity_account_day` VALUES (213, 'mawnuy_hce', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:01', '2025-09-11 20:43:01');
INSERT INTO `raffle_activity_account_day` VALUES (214, 'pd6fvp_ssq', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:02', '2025-09-11 20:43:02');
INSERT INTO `raffle_activity_account_day` VALUES (215, 'tw6my276', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:03', '2025-09-11 20:43:03');
INSERT INTO `raffle_activity_account_day` VALUES (216, 'mzitue_nj340', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:03', '2025-09-11 20:43:03');
INSERT INTO `raffle_activity_account_day` VALUES (217, 'pa8kfk6', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:04', '2025-09-11 20:43:04');
INSERT INTO `raffle_activity_account_day` VALUES (218, 'k8qi48_tml1', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:05', '2025-09-11 20:43:05');
INSERT INTO `raffle_activity_account_day` VALUES (219, 'm8eiuk_g3f', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:06', '2025-09-11 20:43:06');
INSERT INTO `raffle_activity_account_day` VALUES (220, 'ktzmzi_scp', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:07', '2025-09-11 20:43:07');
INSERT INTO `raffle_activity_account_day` VALUES (221, 'n0emzg_tn27', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:08', '2025-09-11 20:43:08');
INSERT INTO `raffle_activity_account_day` VALUES (222, 'k1zsbt_l9x91', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:08', '2025-09-11 20:43:08');
INSERT INTO `raffle_activity_account_day` VALUES (223, 'lmfmhh_kd7', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:09', '2025-09-11 20:43:09');
INSERT INTO `raffle_activity_account_day` VALUES (224, 'k5qsbt.oar93', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:10', '2025-09-11 20:43:10');
INSERT INTO `raffle_activity_account_day` VALUES (225, 'k18iui_tn253', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:11', '2025-09-11 20:43:11');
INSERT INTO `raffle_activity_account_day` VALUES (226, 'k18iui_ist', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:11', '2025-09-11 20:43:11');
INSERT INTO `raffle_activity_account_day` VALUES (227, 'rydkjv_qj262', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:12', '2025-09-11 20:43:12');
INSERT INTO `raffle_activity_account_day` VALUES (228, 'i1cl5v.ioq82', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:13', '2025-09-11 20:43:13');
INSERT INTO `raffle_activity_account_day` VALUES (229, 'v9rmdm75', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:14', '2025-09-11 20:43:14');
INSERT INTO `raffle_activity_account_day` VALUES (230, 'hm1nuy_tt6', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:15', '2025-09-11 20:43:15');
INSERT INTO `raffle_activity_account_day` VALUES (231, 'ggqk1z.fj980', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:16', '2025-09-11 20:43:16');
INSERT INTO `raffle_activity_account_day` VALUES (232, 'ryilj19', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:16', '2025-09-11 20:43:16');
INSERT INTO `raffle_activity_account_day` VALUES (233, 'ppm_sks30', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:17', '2025-09-11 20:43:17');
INSERT INTO `raffle_activity_account_day` VALUES (234, 'hm1i2v26', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:18', '2025-09-11 20:43:18');
INSERT INTO `raffle_activity_account_day` VALUES (235, 'ffxlmf_lrs54', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:19', '2025-09-11 20:43:19');
INSERT INTO `raffle_activity_account_day` VALUES (236, 'ggqk1z31', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:20', '2025-09-11 20:43:20');
INSERT INTO `raffle_activity_account_day` VALUES (237, 'ts5hvb_ssq', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:20', '2025-09-11 20:43:20');
INSERT INTO `raffle_activity_account_day` VALUES (238, 'lh7lnf.g53', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:21', '2025-09-11 20:43:21');
INSERT INTO `raffle_activity_account_day` VALUES (239, 'lh7k1z_i46', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:22', '2025-09-11 20:43:22');
INSERT INTO `raffle_activity_account_day` VALUES (240, 'mawfmn57', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:23', '2025-09-11 20:43:23');
INSERT INTO `raffle_activity_account_day` VALUES (241, 'pybsbt.fnq', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:24', '2025-09-11 20:43:24');
INSERT INTO `raffle_activity_account_day` VALUES (242, 'i8lv9r.jdb', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:25', '2025-09-11 20:43:25');
INSERT INTO `raffle_activity_account_day` VALUES (243, 'gs2k34_nez', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:26', '2025-09-11 20:43:26');
INSERT INTO `raffle_activity_account_day` VALUES (244, 'ts5hvb.q7z14', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:26', '2025-09-11 20:43:26');
INSERT INTO `raffle_activity_account_day` VALUES (245, 'tw9izl_puf', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:27', '2025-09-11 20:43:27');
INSERT INTO `raffle_activity_account_day` VALUES (246, 'i1cvdk.sk766', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:28', '2025-09-11 20:43:28');
INSERT INTO `raffle_activity_account_day` VALUES (247, 'is8k1z_fgs', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:29', '2025-09-11 20:43:29');
INSERT INTO `raffle_activity_account_day` VALUES (248, 'jcvsbt87', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:29', '2025-09-11 20:43:29');
INSERT INTO `raffle_activity_account_day` VALUES (249, 'gs2k34_u4f88', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:30', '2025-09-11 20:43:30');
INSERT INTO `raffle_activity_account_day` VALUES (250, 'k5mmcx74', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:31', '2025-09-11 20:43:31');
INSERT INTO `raffle_activity_account_day` VALUES (251, 'k5mmcx.qg973', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:32', '2025-09-11 20:43:32');
INSERT INTO `raffle_activity_account_day` VALUES (252, 'oq3kgn.gl4o6y10', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:33', '2025-09-11 20:43:33');
INSERT INTO `raffle_activity_account_day` VALUES (253, 'k7ngvs.gky42', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:33', '2025-09-11 20:43:33');
INSERT INTO `raffle_activity_account_day` VALUES (254, 'ffxlmf_gqg33', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:34', '2025-09-11 20:43:34');
INSERT INTO `raffle_activity_account_day` VALUES (255, 'fvpts4_qg435', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:35', '2025-09-11 20:43:35');
INSERT INTO `raffle_activity_account_day` VALUES (256, 'lm1i2v63', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:36', '2025-09-11 20:43:36');
INSERT INTO `raffle_activity_account_day` VALUES (257, 'i1clo5_oti', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:37', '2025-09-11 20:43:37');
INSERT INTO `raffle_activity_account_day` VALUES (258, 'oq3kgn39', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:37', '2025-09-11 20:43:37');
INSERT INTO `raffle_activity_account_day` VALUES (259, 'fmnnzu.sib', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:38', '2025-09-11 20:43:38');
INSERT INTO `raffle_activity_account_day` VALUES (260, 'k18iui23', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:39', '2025-09-11 20:43:39');
INSERT INTO `raffle_activity_account_day` VALUES (261, 'hm1nuy_lid', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:40', '2025-09-11 20:43:40');
INSERT INTO `raffle_activity_account_day` VALUES (262, 'jlbffg18', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:41', '2025-09-11 20:43:41');
INSERT INTO `raffle_activity_account_day` VALUES (263, 'pgknz911', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:41', '2025-09-11 20:43:41');
INSERT INTO `raffle_activity_account_day` VALUES (264, 'k1zu0c_skj34', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:42', '2025-09-11 20:43:42');
INSERT INTO `raffle_activity_account_day` VALUES (265, 'jlbffg20', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:43', '2025-09-11 20:43:43');
INSERT INTO `raffle_activity_account_day` VALUES (266, 'vacsbt86', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:44', '2025-09-11 20:43:44');
INSERT INTO `raffle_activity_account_day` VALUES (267, 'v9bmdm.s3r72', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:45', '2025-09-11 20:43:45');
INSERT INTO `raffle_activity_account_day` VALUES (268, 'jzzgm3_ntz', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:45', '2025-09-11 20:43:45');
INSERT INTO `raffle_activity_account_day` VALUES (269, 'k8qlqi.nel95', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:46', '2025-09-11 20:43:46');
INSERT INTO `raffle_activity_account_day` VALUES (270, 'gztmft.i85', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:47', '2025-09-11 20:43:47');
INSERT INTO `raffle_activity_account_day` VALUES (271, 'izhlu8_kgs62', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:48', '2025-09-11 20:43:48');
INSERT INTO `raffle_activity_account_day` VALUES (272, 'sf0ppm_onw', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:48', '2025-09-11 20:43:48');
INSERT INTO `raffle_activity_account_day` VALUES (273, 'u6mgge_sfi', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:49', '2025-09-11 20:43:49');
INSERT INTO `raffle_activity_account_day` VALUES (274, 'hm1hs7_fep', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:50', '2025-09-11 20:43:50');
INSERT INTO `raffle_activity_account_day` VALUES (275, 'mzitue_lzw', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:51', '2025-09-11 20:43:51');
INSERT INTO `raffle_activity_account_day` VALUES (276, 'fgwuih.gkv94', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:52', '2025-09-11 20:43:52');
INSERT INTO `raffle_activity_account_day` VALUES (277, 'ttbtw9_hlm32', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:53', '2025-09-11 20:43:53');
INSERT INTO `raffle_activity_account_day` VALUES (278, 'me4nzu15', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:53', '2025-09-11 20:43:53');
INSERT INTO `raffle_activity_account_day` VALUES (279, 'lm1_tti', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:54', '2025-09-11 20:43:54');
INSERT INTO `raffle_activity_account_day` VALUES (280, 'jay', 100301, '2025-09-12', 1200, 1092, '2025-09-12 12:14:33', '2025-09-12 12:14:33');

-- ----------------------------
-- Table structure for raffle_activity_account_month
-- ----------------------------
DROP TABLE IF EXISTS `raffle_activity_account_month`;
CREATE TABLE `raffle_activity_account_month`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `month` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '月（yyyy-mm）',
  `month_count` int NOT NULL COMMENT '月次数',
  `month_count_surplus` int NOT NULL COMMENT '月次数-剩余',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_user_id_activity_id_month`(`user_id` ASC, `activity_id` ASC, `month` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 279 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抽奖活动账户表-月次数' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of raffle_activity_account_month
-- ----------------------------
INSERT INTO `raffle_activity_account_month` VALUES (11, 'Jay', 100301, '2025-09', 1200, 1092, '2025-09-11 15:35:41', '2025-09-12 12:14:33');
INSERT INTO `raffle_activity_account_month` VALUES (12, 'fvpki7.keb22', 100301, '2025-09', 1000, 992, '2025-09-11 20:22:02', '2025-09-11 20:40:32');
INSERT INTO `raffle_activity_account_month` VALUES (13, 'ixjlj1_fgs', 100301, '2025-09', 1000, 993, '2025-09-11 20:37:05', '2025-09-11 20:40:33');
INSERT INTO `raffle_activity_account_month` VALUES (16, 'v9bmdm98', 100301, '2025-09', 1000, 997, '2025-09-11 20:37:06', '2025-09-11 20:40:34');
INSERT INTO `raffle_activity_account_month` VALUES (19, 'l7nrqi39', 100301, '2025-09', 1000, 998, '2025-09-11 20:37:06', '2025-09-11 20:40:35');
INSERT INTO `raffle_activity_account_month` VALUES (20, 'tbfv9r_l7q84', 100301, '2025-09', 1000, 998, '2025-09-11 20:37:07', '2025-09-11 20:40:35');
INSERT INTO `raffle_activity_account_month` VALUES (23, 'jwupu5.gjv', 100301, '2025-09', 1000, 998, '2025-09-11 20:37:08', '2025-09-11 20:40:36');
INSERT INTO `raffle_activity_account_month` VALUES (24, 'lh7lnf.iul17', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:37', '2025-09-11 20:40:37');
INSERT INTO `raffle_activity_account_month` VALUES (25, 'p72k3d55', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:37', '2025-09-11 20:40:37');
INSERT INTO `raffle_activity_account_month` VALUES (26, 'izhfoz52', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:38', '2025-09-11 20:40:38');
INSERT INTO `raffle_activity_account_month` VALUES (27, 'jlbi46_jew', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:39', '2025-09-11 20:40:39');
INSERT INTO `raffle_activity_account_month` VALUES (28, 'vdak5x51', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:39', '2025-09-11 20:40:39');
INSERT INTO `raffle_activity_account_month` VALUES (29, 'k1zsbt76', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:40', '2025-09-11 20:40:40');
INSERT INTO `raffle_activity_account_month` VALUES (30, 'v9blnf12', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:41', '2025-09-11 20:40:41');
INSERT INTO `raffle_activity_account_month` VALUES (31, 'ggqryd_skd44', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:42', '2025-09-11 20:40:42');
INSERT INTO `raffle_activity_account_month` VALUES (32, 'fsekfk.jh485', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:42', '2025-09-11 20:40:42');
INSERT INTO `raffle_activity_account_month` VALUES (33, 'k8qkfk86', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:43', '2025-09-11 20:40:43');
INSERT INTO `raffle_activity_account_month` VALUES (34, 'pd6fvp_kd762', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:44', '2025-09-11 20:40:44');
INSERT INTO `raffle_activity_account_month` VALUES (35, 'irescp24', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:44', '2025-09-11 20:40:44');
INSERT INTO `raffle_activity_account_month` VALUES (36, 'lm1_p1k', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:45', '2025-09-11 20:40:45');
INSERT INTO `raffle_activity_account_month` VALUES (37, 'tgmo3v_tg9', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:46', '2025-09-11 20:40:46');
INSERT INTO `raffle_activity_account_month` VALUES (38, 'sf0ppm47', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:46', '2025-09-11 20:40:46');
INSERT INTO `raffle_activity_account_month` VALUES (39, 'pgknz9.q5795', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:47', '2025-09-11 20:40:47');
INSERT INTO `raffle_activity_account_month` VALUES (40, 'gs2m5650', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:48', '2025-09-11 20:40:48');
INSERT INTO `raffle_activity_account_month` VALUES (41, 'jzzgm341', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:48', '2025-09-11 20:40:48');
INSERT INTO `raffle_activity_account_month` VALUES (42, 'k8qscp_glx72', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:49', '2025-09-11 20:40:49');
INSERT INTO `raffle_activity_account_month` VALUES (43, 'i2xmy224', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:50', '2025-09-11 20:40:50');
INSERT INTO `raffle_activity_account_month` VALUES (44, 'nlrlqi_j7y', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:50', '2025-09-11 20:40:50');
INSERT INTO `raffle_activity_account_month` VALUES (45, 'ryilj149', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:51', '2025-09-11 20:40:51');
INSERT INTO `raffle_activity_account_month` VALUES (46, 'mzitue_ol733', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:52', '2025-09-11 20:40:52');
INSERT INTO `raffle_activity_account_month` VALUES (47, 'ixjlj1_tqo', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:52', '2025-09-11 20:40:52');
INSERT INTO `raffle_activity_account_month` VALUES (48, 'lm1i2v83', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:53', '2025-09-11 20:40:53');
INSERT INTO `raffle_activity_account_month` VALUES (49, 'ixjit6.laj61', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:54', '2025-09-11 20:40:54');
INSERT INTO `raffle_activity_account_month` VALUES (50, 'm97k5q.ny968', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:54', '2025-09-11 20:40:54');
INSERT INTO `raffle_activity_account_month` VALUES (51, 'ui8_n1l68', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:55', '2025-09-11 20:40:55');
INSERT INTO `raffle_activity_account_month` VALUES (52, 'tt6kfy43', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:56', '2025-09-11 20:40:56');
INSERT INTO `raffle_activity_account_month` VALUES (53, 'k1z74', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:56', '2025-09-11 20:40:56');
INSERT INTO `raffle_activity_account_month` VALUES (54, 'oq3kgn38', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:57', '2025-09-11 20:40:57');
INSERT INTO `raffle_activity_account_month` VALUES (55, 'kgs_man44', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:58', '2025-09-11 20:40:58');
INSERT INTO `raffle_activity_account_month` VALUES (56, 'i1csbt_gqo91', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:58', '2025-09-11 20:40:58');
INSERT INTO `raffle_activity_account_month` VALUES (57, 'ts5hvb.skx', 100301, '2025-09', 1000, 999, '2025-09-11 20:40:59', '2025-09-11 20:40:59');
INSERT INTO `raffle_activity_account_month` VALUES (58, 'm8eiuk_fl0', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:00', '2025-09-11 20:41:00');
INSERT INTO `raffle_activity_account_month` VALUES (59, 'k8qkfk.sks1', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:00', '2025-09-11 20:41:00');
INSERT INTO `raffle_activity_account_month` VALUES (60, 'ui865', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:01', '2025-09-11 20:41:01');
INSERT INTO `raffle_activity_account_month` VALUES (61, 'i2xmy2_fj6', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:02', '2025-09-11 20:41:02');
INSERT INTO `raffle_activity_account_month` VALUES (62, 'izhpd6_jwt19', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:03', '2025-09-11 20:41:03');
INSERT INTO `raffle_activity_account_month` VALUES (63, 'tw6my286', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:03', '2025-09-11 20:41:03');
INSERT INTO `raffle_activity_account_month` VALUES (64, 'pa8kfk35', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:04', '2025-09-11 20:41:04');
INSERT INTO `raffle_activity_account_month` VALUES (65, 'tt6kfy.i809', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:05', '2025-09-11 20:41:05');
INSERT INTO `raffle_activity_account_month` VALUES (66, 'i1cvdk88', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:05', '2025-09-11 20:41:05');
INSERT INTO `raffle_activity_account_month` VALUES (67, 'mawfmn82', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:06', '2025-09-11 20:41:06');
INSERT INTO `raffle_activity_account_month` VALUES (68, 'fmnrmi_ff1', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:07', '2025-09-11 20:41:07');
INSERT INTO `raffle_activity_account_month` VALUES (69, 'me4nzu_ph932', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:07', '2025-09-11 20:41:07');
INSERT INTO `raffle_activity_account_month` VALUES (70, 'tbfv9r_ssq', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:08', '2025-09-11 20:41:08');
INSERT INTO `raffle_activity_account_month` VALUES (71, 'v9rlnf.gbl49', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:09', '2025-09-11 20:41:09');
INSERT INTO `raffle_activity_account_month` VALUES (72, 'k5mhm1_ky2', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:09', '2025-09-11 20:41:09');
INSERT INTO `raffle_activity_account_month` VALUES (73, 'k5qkfk.qap8', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:10', '2025-09-11 20:41:10');
INSERT INTO `raffle_activity_account_month` VALUES (74, 'pu9ggq.gc3', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:11', '2025-09-11 20:41:11');
INSERT INTO `raffle_activity_account_month` VALUES (75, 'rmftt492', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:11', '2025-09-11 20:41:11');
INSERT INTO `raffle_activity_account_month` VALUES (76, 'tt6kfy.lly', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:12', '2025-09-11 20:41:12');
INSERT INTO `raffle_activity_account_month` VALUES (77, 'ui859', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:13', '2025-09-11 20:41:13');
INSERT INTO `raffle_activity_account_month` VALUES (78, 'v9ru724', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:13', '2025-09-11 20:41:13');
INSERT INTO `raffle_activity_account_month` VALUES (79, 'hm1nuy43', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:14', '2025-09-11 20:41:14');
INSERT INTO `raffle_activity_account_month` VALUES (80, 'nlrlqi66', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:15', '2025-09-11 20:41:15');
INSERT INTO `raffle_activity_account_month` VALUES (81, 'fevfoz.gne23', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:16', '2025-09-11 20:41:16');
INSERT INTO `raffle_activity_account_month` VALUES (82, 'k8qi48.pip39', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:17', '2025-09-11 20:41:17');
INSERT INTO `raffle_activity_account_month` VALUES (83, 'mzitue.m2hgkw', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:17', '2025-09-11 20:41:17');
INSERT INTO `raffle_activity_account_month` VALUES (84, 'izhlq4.lpvfim58', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:18', '2025-09-11 20:41:18');
INSERT INTO `raffle_activity_account_month` VALUES (85, 'lh7lnf96', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:19', '2025-09-11 20:41:19');
INSERT INTO `raffle_activity_account_month` VALUES (86, 'fh5q31.ljf', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:20', '2025-09-11 20:41:20');
INSERT INTO `raffle_activity_account_month` VALUES (87, 'gs2k34_ny930', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:21', '2025-09-11 20:41:21');
INSERT INTO `raffle_activity_account_month` VALUES (88, 'mawtue37', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:21', '2025-09-11 20:41:21');
INSERT INTO `raffle_activity_account_month` VALUES (89, 'jwupu524', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:22', '2025-09-11 20:41:22');
INSERT INTO `raffle_activity_account_month` VALUES (90, 'pu9ggq_h5q81', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:23', '2025-09-11 20:41:23');
INSERT INTO `raffle_activity_account_month` VALUES (91, 'hm1nuy_frz89', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:24', '2025-09-11 20:41:24');
INSERT INTO `raffle_activity_account_month` VALUES (92, 'tgmo3v.tfa99', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:25', '2025-09-11 20:41:25');
INSERT INTO `raffle_activity_account_month` VALUES (93, 'k1zu0c.gbf58', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:26', '2025-09-11 20:41:26');
INSERT INTO `raffle_activity_account_month` VALUES (94, 'id7mft99', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:26', '2025-09-11 20:41:26');
INSERT INTO `raffle_activity_account_month` VALUES (95, 'tbfnuy58', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:27', '2025-09-11 20:41:27');
INSERT INTO `raffle_activity_account_month` VALUES (96, 'ffxlmf_l9v84', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:28', '2025-09-11 20:41:28');
INSERT INTO `raffle_activity_account_month` VALUES (97, 'fmnrmi_ptd', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:29', '2025-09-11 20:41:29');
INSERT INTO `raffle_activity_account_month` VALUES (98, 'tt4lj1_vix54', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:30', '2025-09-11 20:41:30');
INSERT INTO `raffle_activity_account_month` VALUES (99, 'irescp12', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:30', '2025-09-11 20:41:30');
INSERT INTO `raffle_activity_account_month` VALUES (100, 'ttbtw992', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:31', '2025-09-11 20:41:31');
INSERT INTO `raffle_activity_account_month` VALUES (101, 'fozmy264', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:32', '2025-09-11 20:41:32');
INSERT INTO `raffle_activity_account_month` VALUES (102, 'rydkjv.g3k46', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:33', '2025-09-11 20:41:33');
INSERT INTO `raffle_activity_account_month` VALUES (103, 'fgwuih10', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:33', '2025-09-11 20:41:33');
INSERT INTO `raffle_activity_account_month` VALUES (104, 'gs2m5634', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:34', '2025-09-11 20:41:34');
INSERT INTO `raffle_activity_account_month` VALUES (105, 'mawnuy3', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:35', '2025-09-11 20:41:35');
INSERT INTO `raffle_activity_account_month` VALUES (106, 'mawfmn_rlk', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:36', '2025-09-11 20:41:36');
INSERT INTO `raffle_activity_account_month` VALUES (107, 'izhfoz96', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:37', '2025-09-11 20:41:37');
INSERT INTO `raffle_activity_account_month` VALUES (108, 'p72k3d.rvz', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:37', '2025-09-11 20:41:37');
INSERT INTO `raffle_activity_account_month` VALUES (109, 'izhpd6_lxi', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:38', '2025-09-11 20:41:38');
INSERT INTO `raffle_activity_account_month` VALUES (110, 'jzzgm374', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:39', '2025-09-11 20:41:39');
INSERT INTO `raffle_activity_account_month` VALUES (111, 'i1cuji.nev32', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:40', '2025-09-11 20:41:40');
INSERT INTO `raffle_activity_account_month` VALUES (112, 'lj1ljf_g8b10', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:41', '2025-09-11 20:41:41');
INSERT INTO `raffle_activity_account_month` VALUES (113, 'hm1nuy.lqd', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:41', '2025-09-11 20:41:41');
INSERT INTO `raffle_activity_account_month` VALUES (114, 'k1zggq.o34', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:42', '2025-09-11 20:41:42');
INSERT INTO `raffle_activity_account_month` VALUES (115, 'v9rmdm_gp82', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:43', '2025-09-11 20:41:43');
INSERT INTO `raffle_activity_account_month` VALUES (116, 'o97scp88', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:44', '2025-09-11 20:41:44');
INSERT INTO `raffle_activity_account_month` VALUES (117, 'izhlq423', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:45', '2025-09-11 20:41:45');
INSERT INTO `raffle_activity_account_month` VALUES (118, 'fextfv_hqj', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:46', '2025-09-11 20:41:46');
INSERT INTO `raffle_activity_account_month` VALUES (119, 'p1pvgw84', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:46', '2025-09-11 20:41:46');
INSERT INTO `raffle_activity_account_month` VALUES (120, 'iuk.ptd72', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:47', '2025-09-11 20:41:47');
INSERT INTO `raffle_activity_account_month` VALUES (121, 'jwui2v_n5e', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:48', '2025-09-11 20:41:48');
INSERT INTO `raffle_activity_account_month` VALUES (122, 'kutqrx_lal54', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:49', '2025-09-11 20:41:49');
INSERT INTO `raffle_activity_account_month` VALUES (123, 'p72k3d43', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:49', '2025-09-11 20:41:49');
INSERT INTO `raffle_activity_account_month` VALUES (124, 'rmftt4.k28', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:50', '2025-09-11 20:41:50');
INSERT INTO `raffle_activity_account_month` VALUES (125, 'i1cvdk31', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:51', '2025-09-11 20:41:51');
INSERT INTO `raffle_activity_account_month` VALUES (126, 'k8qscp97', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:52', '2025-09-11 20:41:52');
INSERT INTO `raffle_activity_account_month` VALUES (127, 'ixjlj1_kk17', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:53', '2025-09-11 20:41:53');
INSERT INTO `raffle_activity_account_month` VALUES (128, 'k5mmcx_ny262', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:53', '2025-09-11 20:41:53');
INSERT INTO `raffle_activity_account_month` VALUES (129, 'k18to3.lg016', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:54', '2025-09-11 20:41:54');
INSERT INTO `raffle_activity_account_month` VALUES (130, 'p72k3d.kjv', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:55', '2025-09-11 20:41:55');
INSERT INTO `raffle_activity_account_month` VALUES (131, 'o97scp33', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:56', '2025-09-11 20:41:56');
INSERT INTO `raffle_activity_account_month` VALUES (132, 'k5mit6.fk780', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:57', '2025-09-11 20:41:57');
INSERT INTO `raffle_activity_account_month` VALUES (133, 'k5q_io462', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:58', '2025-09-11 20:41:58');
INSERT INTO `raffle_activity_account_month` VALUES (134, 'gztmft22', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:58', '2025-09-11 20:41:58');
INSERT INTO `raffle_activity_account_month` VALUES (135, 'ixjit6_tti32', 100301, '2025-09', 1000, 999, '2025-09-11 20:41:59', '2025-09-11 20:41:59');
INSERT INTO `raffle_activity_account_month` VALUES (136, 'fevfoz.kfz23', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:00', '2025-09-11 20:42:00');
INSERT INTO `raffle_activity_account_month` VALUES (137, 'oq3kgn_hge', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:01', '2025-09-11 20:42:01');
INSERT INTO `raffle_activity_account_month` VALUES (138, 'k7fv9r_fgw', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:02', '2025-09-11 20:42:02');
INSERT INTO `raffle_activity_account_month` VALUES (139, 'gs2m5656', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:02', '2025-09-11 20:42:02');
INSERT INTO `raffle_activity_account_month` VALUES (140, 'nlrlqi.p1k', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:03', '2025-09-11 20:42:03');
INSERT INTO `raffle_activity_account_month` VALUES (141, 'ucop8d_i4y87', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:04', '2025-09-11 20:42:04');
INSERT INTO `raffle_activity_account_month` VALUES (142, 'sf0ppm57', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:05', '2025-09-11 20:42:05');
INSERT INTO `raffle_activity_account_month` VALUES (143, 'ttbtw9_lr4', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:06', '2025-09-11 20:42:06');
INSERT INTO `raffle_activity_account_month` VALUES (144, 'k88to334', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:07', '2025-09-11 20:42:07');
INSERT INTO `raffle_activity_account_month` VALUES (145, 'ttbtw9.i3e86', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:07', '2025-09-11 20:42:07');
INSERT INTO `raffle_activity_account_month` VALUES (146, 'izhlq4.oti', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:08', '2025-09-11 20:42:08');
INSERT INTO `raffle_activity_account_month` VALUES (147, 'v9r61', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:09', '2025-09-11 20:42:09');
INSERT INTO `raffle_activity_account_month` VALUES (148, 'lm1sds_rkx', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:10', '2025-09-11 20:42:10');
INSERT INTO `raffle_activity_account_month` VALUES (149, 'ffxlmf_fk177', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:11', '2025-09-11 20:42:11');
INSERT INTO `raffle_activity_account_month` VALUES (150, 'knnk88.n5fi8w64', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:11', '2025-09-11 20:42:11');
INSERT INTO `raffle_activity_account_month` VALUES (151, 'tt4lj1.fze59', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:12', '2025-09-11 20:42:12');
INSERT INTO `raffle_activity_account_month` VALUES (152, 'k8qkfk.p9c', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:13', '2025-09-11 20:42:13');
INSERT INTO `raffle_activity_account_month` VALUES (153, 'ryilj139', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:14', '2025-09-11 20:42:14');
INSERT INTO `raffle_activity_account_month` VALUES (154, 'ttbtw9_gxn24', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:15', '2025-09-11 20:42:15');
INSERT INTO `raffle_activity_account_month` VALUES (155, 'v9rlnf_iwn40', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:15', '2025-09-11 20:42:15');
INSERT INTO `raffle_activity_account_month` VALUES (156, 'k5qkfk_tj383', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:16', '2025-09-11 20:42:16');
INSERT INTO `raffle_activity_account_month` VALUES (157, 'o97scp_i0k11', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:17', '2025-09-11 20:42:17');
INSERT INTO `raffle_activity_account_month` VALUES (158, 'k7znlr.s3r', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:18', '2025-09-11 20:42:18');
INSERT INTO `raffle_activity_account_month` VALUES (159, 'gs2m5680', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:19', '2025-09-11 20:42:19');
INSERT INTO `raffle_activity_account_month` VALUES (160, 'fmni4832', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:20', '2025-09-11 20:42:20');
INSERT INTO `raffle_activity_account_month` VALUES (161, 'o97scp87', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:20', '2025-09-11 20:42:20');
INSERT INTO `raffle_activity_account_month` VALUES (162, 'hm1i2v_sp2', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:21', '2025-09-11 20:42:21');
INSERT INTO `raffle_activity_account_month` VALUES (163, 'k5mmcx.jew', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:22', '2025-09-11 20:42:22');
INSERT INTO `raffle_activity_account_month` VALUES (164, 'tdcsbt.sbq1', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:22', '2025-09-11 20:42:22');
INSERT INTO `raffle_activity_account_month` VALUES (165, 'fh5q31_irk', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:23', '2025-09-11 20:42:23');
INSERT INTO `raffle_activity_account_month` VALUES (166, 'lh7lnf69', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:24', '2025-09-11 20:42:24');
INSERT INTO `raffle_activity_account_month` VALUES (167, 'fvpki7.ghc52', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:25', '2025-09-11 20:42:25');
INSERT INTO `raffle_activity_account_month` VALUES (168, 'tbfnuy40', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:25', '2025-09-11 20:42:25');
INSERT INTO `raffle_activity_account_month` VALUES (169, 'k5mmcx.n5fi8w', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:26', '2025-09-11 20:42:26');
INSERT INTO `raffle_activity_account_month` VALUES (170, 'gs2m56_tqo', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:27', '2025-09-11 20:42:27');
INSERT INTO `raffle_activity_account_month` VALUES (171, 'gs2k34_i3h8', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:28', '2025-09-11 20:42:28');
INSERT INTO `raffle_activity_account_month` VALUES (172, 'jcvsbt_rx7', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:29', '2025-09-11 20:42:29');
INSERT INTO `raffle_activity_account_month` VALUES (173, 'me4hce.nyg4', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:30', '2025-09-11 20:42:30');
INSERT INTO `raffle_activity_account_month` VALUES (174, 'mzitue.o2g87', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:30', '2025-09-11 20:42:30');
INSERT INTO `raffle_activity_account_month` VALUES (175, 'k88to3.tqj', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:31', '2025-09-11 20:42:31');
INSERT INTO `raffle_activity_account_month` VALUES (176, 'ui839', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:32', '2025-09-11 20:42:32');
INSERT INTO `raffle_activity_account_month` VALUES (177, 'lnf_ggu', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:33', '2025-09-11 20:42:33');
INSERT INTO `raffle_activity_account_month` VALUES (178, 'k8qkfk_ffg84', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:34', '2025-09-11 20:42:34');
INSERT INTO `raffle_activity_account_month` VALUES (179, 'k5qscp59', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:34', '2025-09-11 20:42:34');
INSERT INTO `raffle_activity_account_month` VALUES (180, 'lm1sbt6', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:35', '2025-09-11 20:42:35');
INSERT INTO `raffle_activity_account_month` VALUES (181, 'vacsbt97', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:36', '2025-09-11 20:42:36');
INSERT INTO `raffle_activity_account_month` VALUES (182, 'po4sf0_ib777', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:37', '2025-09-11 20:42:37');
INSERT INTO `raffle_activity_account_month` VALUES (183, 'lmfmhh.ire77', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:38', '2025-09-11 20:42:38');
INSERT INTO `raffle_activity_account_month` VALUES (184, 'ppm.nz1', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:38', '2025-09-11 20:42:38');
INSERT INTO `raffle_activity_account_month` VALUES (185, 'i1cvdk65', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:39', '2025-09-11 20:42:39');
INSERT INTO `raffle_activity_account_month` VALUES (186, 'k88to3_kfc', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:40', '2025-09-11 20:42:40');
INSERT INTO `raffle_activity_account_month` VALUES (187, 'sf0ppm.k2860', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:41', '2025-09-11 20:42:41');
INSERT INTO `raffle_activity_account_month` VALUES (188, 'ggqryd44', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:41', '2025-09-11 20:42:41');
INSERT INTO `raffle_activity_account_month` VALUES (189, 'pybsbt30', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:42', '2025-09-11 20:42:42');
INSERT INTO `raffle_activity_account_month` VALUES (190, 'tt6kfy.g4v78', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:43', '2025-09-11 20:42:43');
INSERT INTO `raffle_activity_account_month` VALUES (191, 'ucop8d.glk', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:44', '2025-09-11 20:42:44');
INSERT INTO `raffle_activity_account_month` VALUES (192, 'ryilj186', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:45', '2025-09-11 20:42:45');
INSERT INTO `raffle_activity_account_month` VALUES (193, 'fmnnzu.keb98', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:45', '2025-09-11 20:42:45');
INSERT INTO `raffle_activity_account_month` VALUES (194, 'o97fmn21', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:46', '2025-09-11 20:42:46');
INSERT INTO `raffle_activity_account_month` VALUES (195, 'k7ngvs_gbf', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:47', '2025-09-11 20:42:47');
INSERT INTO `raffle_activity_account_month` VALUES (196, 'ggqryd_i6hsf355', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:48', '2025-09-11 20:42:48');
INSERT INTO `raffle_activity_account_month` VALUES (197, 'i1cuji52', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:49', '2025-09-11 20:42:49');
INSERT INTO `raffle_activity_account_month` VALUES (198, 'pybsbt85', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:50', '2025-09-11 20:42:50');
INSERT INTO `raffle_activity_account_month` VALUES (199, 'lmfmhh_sl5', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:51', '2025-09-11 20:42:51');
INSERT INTO `raffle_activity_account_month` VALUES (200, 'ts5tbf24', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:51', '2025-09-11 20:42:51');
INSERT INTO `raffle_activity_account_month` VALUES (201, 'mzitue_sn143', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:52', '2025-09-11 20:42:52');
INSERT INTO `raffle_activity_account_month` VALUES (202, 'fevfoz.lpi22', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:53', '2025-09-11 20:42:53');
INSERT INTO `raffle_activity_account_month` VALUES (203, 'gs2m56_ggl72', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:54', '2025-09-11 20:42:54');
INSERT INTO `raffle_activity_account_month` VALUES (204, 'mzitue.tcu0', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:55', '2025-09-11 20:42:55');
INSERT INTO `raffle_activity_account_month` VALUES (205, 'izhsf0_go824', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:55', '2025-09-11 20:42:55');
INSERT INTO `raffle_activity_account_month` VALUES (206, 'negtue_psi98', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:56', '2025-09-11 20:42:56');
INSERT INTO `raffle_activity_account_month` VALUES (207, 'izhpd6_iwd81', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:57', '2025-09-11 20:42:57');
INSERT INTO `raffle_activity_account_month` VALUES (208, 'jcvsbt.fh869', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:58', '2025-09-11 20:42:58');
INSERT INTO `raffle_activity_account_month` VALUES (209, 'lzbmd2.fqn', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:59', '2025-09-11 20:42:59');
INSERT INTO `raffle_activity_account_month` VALUES (210, 'o97kgs_ghb18', 100301, '2025-09', 1000, 999, '2025-09-11 20:42:59', '2025-09-11 20:42:59');
INSERT INTO `raffle_activity_account_month` VALUES (211, 'g6nmzi_irk', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:00', '2025-09-11 20:43:00');
INSERT INTO `raffle_activity_account_month` VALUES (212, 'mawnuy_hce', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:01', '2025-09-11 20:43:01');
INSERT INTO `raffle_activity_account_month` VALUES (213, 'pd6fvp_ssq', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:02', '2025-09-11 20:43:02');
INSERT INTO `raffle_activity_account_month` VALUES (214, 'tw6my276', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:03', '2025-09-11 20:43:03');
INSERT INTO `raffle_activity_account_month` VALUES (215, 'mzitue_nj340', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:03', '2025-09-11 20:43:03');
INSERT INTO `raffle_activity_account_month` VALUES (216, 'pa8kfk6', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:04', '2025-09-11 20:43:04');
INSERT INTO `raffle_activity_account_month` VALUES (217, 'k8qi48_tml1', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:05', '2025-09-11 20:43:05');
INSERT INTO `raffle_activity_account_month` VALUES (218, 'm8eiuk_g3f', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:06', '2025-09-11 20:43:06');
INSERT INTO `raffle_activity_account_month` VALUES (219, 'ktzmzi_scp', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:07', '2025-09-11 20:43:07');
INSERT INTO `raffle_activity_account_month` VALUES (220, 'n0emzg_tn27', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:08', '2025-09-11 20:43:08');
INSERT INTO `raffle_activity_account_month` VALUES (221, 'k1zsbt_l9x91', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:08', '2025-09-11 20:43:08');
INSERT INTO `raffle_activity_account_month` VALUES (222, 'lmfmhh_kd7', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:09', '2025-09-11 20:43:09');
INSERT INTO `raffle_activity_account_month` VALUES (223, 'k5qsbt.oar93', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:10', '2025-09-11 20:43:10');
INSERT INTO `raffle_activity_account_month` VALUES (224, 'k18iui_tn253', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:11', '2025-09-11 20:43:11');
INSERT INTO `raffle_activity_account_month` VALUES (225, 'k18iui_ist', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:11', '2025-09-11 20:43:11');
INSERT INTO `raffle_activity_account_month` VALUES (226, 'rydkjv_qj262', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:12', '2025-09-11 20:43:12');
INSERT INTO `raffle_activity_account_month` VALUES (227, 'i1cl5v.ioq82', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:13', '2025-09-11 20:43:13');
INSERT INTO `raffle_activity_account_month` VALUES (228, 'v9rmdm75', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:14', '2025-09-11 20:43:14');
INSERT INTO `raffle_activity_account_month` VALUES (229, 'hm1nuy_tt6', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:15', '2025-09-11 20:43:15');
INSERT INTO `raffle_activity_account_month` VALUES (230, 'ggqk1z.fj980', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:15', '2025-09-11 20:43:15');
INSERT INTO `raffle_activity_account_month` VALUES (231, 'ryilj19', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:16', '2025-09-11 20:43:16');
INSERT INTO `raffle_activity_account_month` VALUES (232, 'ppm_sks30', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:17', '2025-09-11 20:43:17');
INSERT INTO `raffle_activity_account_month` VALUES (233, 'hm1i2v26', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:18', '2025-09-11 20:43:18');
INSERT INTO `raffle_activity_account_month` VALUES (234, 'ffxlmf_lrs54', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:19', '2025-09-11 20:43:19');
INSERT INTO `raffle_activity_account_month` VALUES (235, 'ggqk1z31', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:20', '2025-09-11 20:43:20');
INSERT INTO `raffle_activity_account_month` VALUES (236, 'ts5hvb_ssq', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:20', '2025-09-11 20:43:20');
INSERT INTO `raffle_activity_account_month` VALUES (237, 'lh7lnf.g53', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:21', '2025-09-11 20:43:21');
INSERT INTO `raffle_activity_account_month` VALUES (238, 'lh7k1z_i46', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:22', '2025-09-11 20:43:22');
INSERT INTO `raffle_activity_account_month` VALUES (239, 'mawfmn57', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:23', '2025-09-11 20:43:23');
INSERT INTO `raffle_activity_account_month` VALUES (240, 'pybsbt.fnq', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:24', '2025-09-11 20:43:24');
INSERT INTO `raffle_activity_account_month` VALUES (241, 'i8lv9r.jdb', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:25', '2025-09-11 20:43:25');
INSERT INTO `raffle_activity_account_month` VALUES (242, 'gs2k34_nez', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:26', '2025-09-11 20:43:26');
INSERT INTO `raffle_activity_account_month` VALUES (243, 'ts5hvb.q7z14', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:26', '2025-09-11 20:43:26');
INSERT INTO `raffle_activity_account_month` VALUES (244, 'tw9izl_puf', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:27', '2025-09-11 20:43:27');
INSERT INTO `raffle_activity_account_month` VALUES (245, 'i1cvdk.sk766', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:28', '2025-09-11 20:43:28');
INSERT INTO `raffle_activity_account_month` VALUES (246, 'is8k1z_fgs', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:29', '2025-09-11 20:43:29');
INSERT INTO `raffle_activity_account_month` VALUES (247, 'jcvsbt87', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:29', '2025-09-11 20:43:29');
INSERT INTO `raffle_activity_account_month` VALUES (248, 'gs2k34_u4f88', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:30', '2025-09-11 20:43:30');
INSERT INTO `raffle_activity_account_month` VALUES (249, 'k5mmcx74', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:31', '2025-09-11 20:43:31');
INSERT INTO `raffle_activity_account_month` VALUES (250, 'k5mmcx.qg973', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:32', '2025-09-11 20:43:32');
INSERT INTO `raffle_activity_account_month` VALUES (251, 'oq3kgn.gl4o6y10', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:33', '2025-09-11 20:43:33');
INSERT INTO `raffle_activity_account_month` VALUES (252, 'k7ngvs.gky42', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:33', '2025-09-11 20:43:33');
INSERT INTO `raffle_activity_account_month` VALUES (253, 'ffxlmf_gqg33', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:34', '2025-09-11 20:43:34');
INSERT INTO `raffle_activity_account_month` VALUES (254, 'fvpts4_qg435', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:35', '2025-09-11 20:43:35');
INSERT INTO `raffle_activity_account_month` VALUES (255, 'lm1i2v63', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:36', '2025-09-11 20:43:36');
INSERT INTO `raffle_activity_account_month` VALUES (256, 'i1clo5_oti', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:37', '2025-09-11 20:43:37');
INSERT INTO `raffle_activity_account_month` VALUES (257, 'oq3kgn39', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:37', '2025-09-11 20:43:37');
INSERT INTO `raffle_activity_account_month` VALUES (258, 'fmnnzu.sib', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:38', '2025-09-11 20:43:38');
INSERT INTO `raffle_activity_account_month` VALUES (259, 'k18iui23', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:39', '2025-09-11 20:43:39');
INSERT INTO `raffle_activity_account_month` VALUES (260, 'hm1nuy_lid', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:40', '2025-09-11 20:43:40');
INSERT INTO `raffle_activity_account_month` VALUES (261, 'jlbffg18', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:41', '2025-09-11 20:43:41');
INSERT INTO `raffle_activity_account_month` VALUES (262, 'pgknz911', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:41', '2025-09-11 20:43:41');
INSERT INTO `raffle_activity_account_month` VALUES (263, 'k1zu0c_skj34', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:42', '2025-09-11 20:43:42');
INSERT INTO `raffle_activity_account_month` VALUES (264, 'jlbffg20', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:43', '2025-09-11 20:43:43');
INSERT INTO `raffle_activity_account_month` VALUES (265, 'vacsbt86', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:44', '2025-09-11 20:43:44');
INSERT INTO `raffle_activity_account_month` VALUES (266, 'v9bmdm.s3r72', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:45', '2025-09-11 20:43:45');
INSERT INTO `raffle_activity_account_month` VALUES (267, 'jzzgm3_ntz', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:45', '2025-09-11 20:43:45');
INSERT INTO `raffle_activity_account_month` VALUES (268, 'k8qlqi.nel95', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:46', '2025-09-11 20:43:46');
INSERT INTO `raffle_activity_account_month` VALUES (269, 'gztmft.i85', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:47', '2025-09-11 20:43:47');
INSERT INTO `raffle_activity_account_month` VALUES (270, 'izhlu8_kgs62', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:48', '2025-09-11 20:43:48');
INSERT INTO `raffle_activity_account_month` VALUES (271, 'sf0ppm_onw', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:48', '2025-09-11 20:43:48');
INSERT INTO `raffle_activity_account_month` VALUES (272, 'u6mgge_sfi', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:49', '2025-09-11 20:43:49');
INSERT INTO `raffle_activity_account_month` VALUES (273, 'hm1hs7_fep', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:50', '2025-09-11 20:43:50');
INSERT INTO `raffle_activity_account_month` VALUES (274, 'mzitue_lzw', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:51', '2025-09-11 20:43:51');
INSERT INTO `raffle_activity_account_month` VALUES (275, 'fgwuih.gkv94', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:52', '2025-09-11 20:43:52');
INSERT INTO `raffle_activity_account_month` VALUES (276, 'ttbtw9_hlm32', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:53', '2025-09-11 20:43:53');
INSERT INTO `raffle_activity_account_month` VALUES (277, 'me4nzu15', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:53', '2025-09-11 20:43:53');
INSERT INTO `raffle_activity_account_month` VALUES (278, 'lm1_tti', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:54', '2025-09-11 20:43:54');

-- ----------------------------
-- Table structure for raffle_activity_order
-- ----------------------------
DROP TABLE IF EXISTS `raffle_activity_order`;
CREATE TABLE `raffle_activity_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `activity_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动名称',
  `strategy_id` bigint NOT NULL COMMENT '抽奖策略ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单ID',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `state` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单状态（not_used、used、expire）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_user_id_activity_id`(`user_id` ASC, `activity_id` ASC, `state` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抽奖活动单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of raffle_activity_order
-- ----------------------------

-- ----------------------------
-- Table structure for raffle_activity_order_000
-- ----------------------------
DROP TABLE IF EXISTS `raffle_activity_order_000`;
CREATE TABLE `raffle_activity_order_000`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `sku` bigint NOT NULL COMMENT '商品sku',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `activity_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动名称',
  `strategy_id` bigint NOT NULL COMMENT '抽奖策略ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单ID',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `total_count` int NOT NULL COMMENT '总次数',
  `day_count` int NOT NULL COMMENT '日次数',
  `month_count` int NOT NULL COMMENT '月次数',
  `pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '支付金额【积分】',
  `state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'complete' COMMENT '订单状态（complete）',
  `out_business_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务仿重ID - 外部透传的，确保幂等',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  UNIQUE INDEX `uq_out_business_no`(`out_business_no` ASC) USING BTREE,
  INDEX `idx_user_id_activity_id`(`user_id` ASC, `activity_id` ASC, `state` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抽奖活动单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of raffle_activity_order_000
-- ----------------------------

-- ----------------------------
-- Table structure for raffle_activity_order_001
-- ----------------------------
DROP TABLE IF EXISTS `raffle_activity_order_001`;
CREATE TABLE `raffle_activity_order_001`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `sku` bigint NOT NULL COMMENT '商品sku',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `activity_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动名称',
  `strategy_id` bigint NOT NULL COMMENT '抽奖策略ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单ID',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `total_count` int NOT NULL COMMENT '总次数',
  `day_count` int NOT NULL COMMENT '日次数',
  `month_count` int NOT NULL COMMENT '月次数',
  `pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '支付金额【积分】',
  `state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'complete' COMMENT '订单状态（complete）',
  `out_business_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务仿重ID - 外部透传的，确保幂等',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  UNIQUE INDEX `uq_out_business_no`(`out_business_no` ASC) USING BTREE,
  INDEX `idx_user_id_activity_id`(`user_id` ASC, `activity_id` ASC, `state` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4256 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抽奖活动单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of raffle_activity_order_001
-- ----------------------------

-- ----------------------------
-- Table structure for raffle_activity_order_002
-- ----------------------------
DROP TABLE IF EXISTS `raffle_activity_order_002`;
CREATE TABLE `raffle_activity_order_002`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `sku` bigint NOT NULL COMMENT '商品sku',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `activity_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动名称',
  `strategy_id` bigint NOT NULL COMMENT '抽奖策略ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单ID',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `total_count` int NOT NULL COMMENT '总次数',
  `day_count` int NOT NULL COMMENT '日次数',
  `month_count` int NOT NULL COMMENT '月次数',
  `pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '支付金额【积分】',
  `state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'complete' COMMENT '订单状态（complete）',
  `out_business_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务仿重ID - 外部透传的，确保幂等',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  UNIQUE INDEX `uq_out_business_no`(`out_business_no` ASC) USING BTREE,
  INDEX `idx_user_id_activity_id`(`user_id` ASC, `activity_id` ASC, `state` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抽奖活动单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of raffle_activity_order_002
-- ----------------------------

-- ----------------------------
-- Table structure for raffle_activity_order_003
-- ----------------------------
DROP TABLE IF EXISTS `raffle_activity_order_003`;
CREATE TABLE `raffle_activity_order_003`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `sku` bigint NOT NULL COMMENT '商品sku',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `activity_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动名称',
  `strategy_id` bigint NOT NULL COMMENT '抽奖策略ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单ID',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `total_count` int NOT NULL COMMENT '总次数',
  `day_count` int NOT NULL COMMENT '日次数',
  `month_count` int NOT NULL COMMENT '月次数',
  `pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '支付金额【积分】',
  `state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'complete' COMMENT '订单状态（complete）',
  `out_business_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务仿重ID - 外部透传的，确保幂等',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  UNIQUE INDEX `uq_out_business_no`(`out_business_no` ASC) USING BTREE,
  INDEX `idx_user_id_activity_id`(`user_id` ASC, `activity_id` ASC, `state` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抽奖活动单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of raffle_activity_order_003
-- ----------------------------

-- ----------------------------
-- Table structure for task
-- ----------------------------
DROP TABLE IF EXISTS `task`;
CREATE TABLE `task`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `topic` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息主题',
  `message_id` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '消息编号',
  `message` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息主体',
  `state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'create' COMMENT '任务状态；create-创建、completed-完成、fail-失败',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_message_id`(`message_id` ASC) USING BTREE,
  INDEX `idx_state`(`state` ASC) USING BTREE,
  INDEX `idx_create_time`(`update_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 642 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务表，发送MQ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of task
-- ----------------------------
INSERT INTO `task` VALUES (256, 'Jay', 'send_award', '71985093533', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"123251889509\",\"userId\":\"Jay\"},\"id\":\"71985093533\",\"timestamp\":\"2025-09-11 16:18:01.231\"}', 'completed', '2025-09-11 16:18:01', '2025-09-11 16:18:01');
INSERT INTO `task` VALUES (257, 'Jay', 'send_award', '73534355551', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"047265279257\",\"userId\":\"Jay\"},\"id\":\"73534355551\",\"timestamp\":\"2025-09-11 16:18:11.634\"}', 'completed', '2025-09-11 16:18:12', '2025-09-11 16:18:12');
INSERT INTO `task` VALUES (258, 'jay', 'send_award', '15190643657', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"391754657215\",\"userId\":\"jay\"},\"id\":\"15190643657\",\"timestamp\":\"2025-09-11 16:20:55.866\"}', 'completed', '2025-09-11 16:20:56', '2025-09-11 16:20:56');
INSERT INTO `task` VALUES (259, 'jay', 'send_award', '13855757096', '{\"data\":{\"awardConfig\":\"\",\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"076933735647\",\"userId\":\"jay\"},\"id\":\"13855757096\",\"timestamp\":\"2025-09-11 16:23:21.114\"}', 'completed', '2025-09-11 16:23:21', '2025-09-11 16:23:21');
INSERT INTO `task` VALUES (260, 'jay', 'send_award', '27023151619', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"783289721604\",\"userId\":\"jay\"},\"id\":\"27023151619\",\"timestamp\":\"2025-09-11 16:23:37.754\"}', 'completed', '2025-09-11 16:23:38', '2025-09-11 16:23:38');
INSERT INTO `task` VALUES (261, 'jay', 'send_award', '84132589733', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"033552351982\",\"userId\":\"jay\"},\"id\":\"84132589733\",\"timestamp\":\"2025-09-11 16:24:04.321\"}', 'completed', '2025-09-11 16:24:04', '2025-09-11 16:24:04');
INSERT INTO `task` VALUES (262, 'jay', 'send_award', '06292269967', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"821015638921\",\"userId\":\"jay\"},\"id\":\"06292269967\",\"timestamp\":\"2025-09-11 16:24:04.422\"}', 'completed', '2025-09-11 16:24:04', '2025-09-11 16:24:04');
INSERT INTO `task` VALUES (263, 'jay', 'send_award', '07031785267', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"499159553296\",\"userId\":\"jay\"},\"id\":\"07031785267\",\"timestamp\":\"2025-09-11 16:24:04.513\"}', 'completed', '2025-09-11 16:24:04', '2025-09-11 16:24:05');
INSERT INTO `task` VALUES (264, 'jay', 'send_award', '84983488242', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"046267938929\",\"userId\":\"jay\"},\"id\":\"84983488242\",\"timestamp\":\"2025-09-11 16:24:04.535\"}', 'completed', '2025-09-11 16:24:04', '2025-09-11 16:24:05');
INSERT INTO `task` VALUES (265, 'jay', 'send_award', '33247226982', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"297927773450\",\"userId\":\"jay\"},\"id\":\"33247226982\",\"timestamp\":\"2025-09-11 16:24:04.655\"}', 'completed', '2025-09-11 16:24:05', '2025-09-11 16:24:05');
INSERT INTO `task` VALUES (266, 'jay', 'send_award', '45347970588', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"216637128225\",\"userId\":\"jay\"},\"id\":\"45347970588\",\"timestamp\":\"2025-09-11 16:24:04.829\"}', 'completed', '2025-09-11 16:24:05', '2025-09-11 16:24:05');
INSERT INTO `task` VALUES (267, 'jay', 'send_award', '23802052728', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"205945313683\",\"userId\":\"jay\"},\"id\":\"23802052728\",\"timestamp\":\"2025-09-11 16:24:04.996\"}', 'completed', '2025-09-11 16:24:05', '2025-09-11 16:24:05');
INSERT INTO `task` VALUES (268, 'jay', 'send_award', '72949848702', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"757591364360\",\"userId\":\"jay\"},\"id\":\"72949848702\",\"timestamp\":\"2025-09-11 16:24:05.133\"}', 'completed', '2025-09-11 16:24:05', '2025-09-11 16:24:05');
INSERT INTO `task` VALUES (269, 'jay', 'send_award', '69949922621', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"571613897942\",\"userId\":\"jay\"},\"id\":\"69949922621\",\"timestamp\":\"2025-09-11 16:24:05.369\"}', 'completed', '2025-09-11 16:24:05', '2025-09-11 16:24:05');
INSERT INTO `task` VALUES (270, 'jay', 'send_award', '15233816261', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"546832500103\",\"userId\":\"jay\"},\"id\":\"15233816261\",\"timestamp\":\"2025-09-11 16:24:05.673\"}', 'completed', '2025-09-11 16:24:06', '2025-09-11 16:24:06');
INSERT INTO `task` VALUES (271, 'jay', 'send_award', '57375830425', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"238802359406\",\"userId\":\"jay\"},\"id\":\"57375830425\",\"timestamp\":\"2025-09-11 16:24:05.679\"}', 'completed', '2025-09-11 16:24:06', '2025-09-11 16:24:06');
INSERT INTO `task` VALUES (272, 'jay', 'send_award', '39786711333', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"646916945448\",\"userId\":\"jay\"},\"id\":\"39786711333\",\"timestamp\":\"2025-09-11 16:24:05.82\"}', 'completed', '2025-09-11 16:24:06', '2025-09-11 16:24:06');
INSERT INTO `task` VALUES (273, 'jay', 'send_award', '71916838466', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"194147973545\",\"userId\":\"jay\"},\"id\":\"71916838466\",\"timestamp\":\"2025-09-11 16:24:06.037\"}', 'completed', '2025-09-11 16:24:06', '2025-09-11 16:24:06');
INSERT INTO `task` VALUES (274, 'jay', 'send_award', '22913202440', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"983748839600\",\"userId\":\"jay\"},\"id\":\"22913202440\",\"timestamp\":\"2025-09-11 16:24:06.268\"}', 'completed', '2025-09-11 16:24:06', '2025-09-11 16:24:06');
INSERT INTO `task` VALUES (275, 'jay', 'send_award', '05253894393', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"615094360243\",\"userId\":\"jay\"},\"id\":\"05253894393\",\"timestamp\":\"2025-09-11 16:24:06.393\"}', 'completed', '2025-09-11 16:24:06', '2025-09-11 16:24:06');
INSERT INTO `task` VALUES (276, 'jay', 'send_award', '12831173134', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"652310164400\",\"userId\":\"jay\"},\"id\":\"12831173134\",\"timestamp\":\"2025-09-11 16:24:06.503\"}', 'completed', '2025-09-11 16:24:06', '2025-09-11 16:24:07');
INSERT INTO `task` VALUES (277, 'jay', 'send_award', '71570747874', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"562470757638\",\"userId\":\"jay\"},\"id\":\"71570747874\",\"timestamp\":\"2025-09-11 16:24:06.661\"}', 'completed', '2025-09-11 16:24:07', '2025-09-11 16:24:07');
INSERT INTO `task` VALUES (278, 'jay', 'send_award', '81892841887', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"006241983283\",\"userId\":\"jay\"},\"id\":\"81892841887\",\"timestamp\":\"2025-09-11 16:24:06.838\"}', 'completed', '2025-09-11 16:24:07', '2025-09-11 16:24:07');
INSERT INTO `task` VALUES (279, 'jay', 'send_award', '22979810492', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"552393467348\",\"userId\":\"jay\"},\"id\":\"22979810492\",\"timestamp\":\"2025-09-11 16:24:07.038\"}', 'completed', '2025-09-11 16:24:07', '2025-09-11 16:24:07');
INSERT INTO `task` VALUES (280, 'jay', 'send_award', '08462611529', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"971520963966\",\"userId\":\"jay\"},\"id\":\"08462611529\",\"timestamp\":\"2025-09-11 16:24:07.217\"}', 'completed', '2025-09-11 16:24:07', '2025-09-11 16:24:07');
INSERT INTO `task` VALUES (281, 'jay', 'send_award', '85990996127', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"783707444418\",\"userId\":\"jay\"},\"id\":\"85990996127\",\"timestamp\":\"2025-09-11 16:24:07.693\"}', 'completed', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `task` VALUES (282, 'jay', 'send_award', '78406823380', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"441429068493\",\"userId\":\"jay\"},\"id\":\"78406823380\",\"timestamp\":\"2025-09-11 16:24:07.774\"}', 'completed', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `task` VALUES (283, 'jay', 'send_award', '30988270651', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"237545059288\",\"userId\":\"jay\"},\"id\":\"30988270651\",\"timestamp\":\"2025-09-11 16:24:07.847\"}', 'completed', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `task` VALUES (284, 'jay', 'send_award', '72075403952', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"390428969669\",\"userId\":\"jay\"},\"id\":\"72075403952\",\"timestamp\":\"2025-09-11 16:24:08.002\"}', 'completed', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `task` VALUES (285, 'jay', 'send_award', '87890775518', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"523135197581\",\"userId\":\"jay\"},\"id\":\"87890775518\",\"timestamp\":\"2025-09-11 16:24:08.19\"}', 'completed', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `task` VALUES (286, 'jay', 'send_award', '69263176257', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"443766981895\",\"userId\":\"jay\"},\"id\":\"69263176257\",\"timestamp\":\"2025-09-11 16:24:08.448\"}', 'completed', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `task` VALUES (287, 'jay', 'send_award', '99164671935', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"961659922796\",\"userId\":\"jay\"},\"id\":\"99164671935\",\"timestamp\":\"2025-09-11 16:24:08.617\"}', 'completed', '2025-09-11 16:24:08', '2025-09-11 16:24:09');
INSERT INTO `task` VALUES (288, 'jay', 'send_award', '41409571402', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"091048707105\",\"userId\":\"jay\"},\"id\":\"41409571402\",\"timestamp\":\"2025-09-11 16:24:08.787\"}', 'completed', '2025-09-11 16:24:09', '2025-09-11 16:24:09');
INSERT INTO `task` VALUES (289, 'jay', 'send_award', '09980946793', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"669079823424\",\"userId\":\"jay\"},\"id\":\"09980946793\",\"timestamp\":\"2025-09-11 16:24:08.909\"}', 'completed', '2025-09-11 16:24:09', '2025-09-11 16:24:09');
INSERT INTO `task` VALUES (290, 'jay', 'send_award', '21204021616', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"659624201810\",\"userId\":\"jay\"},\"id\":\"21204021616\",\"timestamp\":\"2025-09-11 16:24:09.085\"}', 'completed', '2025-09-11 16:24:09', '2025-09-11 16:24:09');
INSERT INTO `task` VALUES (291, 'jay', 'send_award', '72501986333', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"283040982965\",\"userId\":\"jay\"},\"id\":\"72501986333\",\"timestamp\":\"2025-09-11 16:24:09.323\"}', 'completed', '2025-09-11 16:24:09', '2025-09-11 16:24:09');
INSERT INTO `task` VALUES (292, 'jay', 'send_award', '47222818226', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"812669656316\",\"userId\":\"jay\"},\"id\":\"47222818226\",\"timestamp\":\"2025-09-11 16:24:09.436\"}', 'completed', '2025-09-11 16:24:09', '2025-09-11 16:24:09');
INSERT INTO `task` VALUES (293, 'jay', 'send_award', '91764009462', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"063456093801\",\"userId\":\"jay\"},\"id\":\"91764009462\",\"timestamp\":\"2025-09-11 16:24:09.549\"}', 'completed', '2025-09-11 16:24:09', '2025-09-11 16:24:10');
INSERT INTO `task` VALUES (294, 'jay', 'send_award', '97144837049', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"307575448159\",\"userId\":\"jay\"},\"id\":\"97144837049\",\"timestamp\":\"2025-09-11 16:24:09.645\"}', 'completed', '2025-09-11 16:24:10', '2025-09-11 16:24:10');
INSERT INTO `task` VALUES (295, 'jay', 'send_award', '99568252379', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"872514825381\",\"userId\":\"jay\"},\"id\":\"99568252379\",\"timestamp\":\"2025-09-11 16:24:09.799\"}', 'completed', '2025-09-11 16:24:10', '2025-09-11 16:24:10');
INSERT INTO `task` VALUES (296, 'jay', 'send_award', '53845913523', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"867998269250\",\"userId\":\"jay\"},\"id\":\"53845913523\",\"timestamp\":\"2025-09-11 16:24:09.953\"}', 'completed', '2025-09-11 16:24:10', '2025-09-11 16:24:10');
INSERT INTO `task` VALUES (297, 'jay', 'send_award', '81808707223', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"455875088253\",\"userId\":\"jay\"},\"id\":\"81808707223\",\"timestamp\":\"2025-09-11 16:24:10.082\"}', 'completed', '2025-09-11 16:24:10', '2025-09-11 16:24:10');
INSERT INTO `task` VALUES (298, 'jay', 'send_award', '09417061563', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"431923721932\",\"userId\":\"jay\"},\"id\":\"09417061563\",\"timestamp\":\"2025-09-11 16:24:10.235\"}', 'completed', '2025-09-11 16:24:10', '2025-09-11 16:24:10');
INSERT INTO `task` VALUES (299, 'jay', 'send_award', '06902326864', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"526176379196\",\"userId\":\"jay\"},\"id\":\"06902326864\",\"timestamp\":\"2025-09-11 16:24:10.394\"}', 'completed', '2025-09-11 16:24:10', '2025-09-11 16:24:10');
INSERT INTO `task` VALUES (300, 'jay', 'send_award', '48317171954', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"539334579100\",\"userId\":\"jay\"},\"id\":\"48317171954\",\"timestamp\":\"2025-09-11 16:25:47.622\"}', 'completed', '2025-09-11 16:25:48', '2025-09-11 16:25:48');
INSERT INTO `task` VALUES (301, 'jay', 'send_award', '27313256317', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"133844269407\",\"userId\":\"jay\"},\"id\":\"27313256317\",\"timestamp\":\"2025-09-11 16:25:47.608\"}', 'completed', '2025-09-11 16:25:48', '2025-09-11 16:25:48');
INSERT INTO `task` VALUES (302, 'jay', 'send_award', '70686422247', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"114307413504\",\"userId\":\"jay\"},\"id\":\"70686422247\",\"timestamp\":\"2025-09-11 16:25:47.704\"}', 'completed', '2025-09-11 16:25:48', '2025-09-11 16:25:48');
INSERT INTO `task` VALUES (303, 'jay', 'send_award', '42788701063', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"793842208878\",\"userId\":\"jay\"},\"id\":\"42788701063\",\"timestamp\":\"2025-09-11 16:25:47.786\"}', 'completed', '2025-09-11 16:25:48', '2025-09-11 16:25:48');
INSERT INTO `task` VALUES (304, 'jay', 'send_award', '70916099207', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"071707115347\",\"userId\":\"jay\"},\"id\":\"70916099207\",\"timestamp\":\"2025-09-11 16:25:47.963\"}', 'completed', '2025-09-11 16:25:48', '2025-09-11 16:25:48');
INSERT INTO `task` VALUES (305, 'jay', 'send_award', '99336681322', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"160343840537\",\"userId\":\"jay\"},\"id\":\"99336681322\",\"timestamp\":\"2025-09-11 16:25:48.135\"}', 'completed', '2025-09-11 16:25:48', '2025-09-11 16:25:48');
INSERT INTO `task` VALUES (306, 'jay', 'send_award', '43216768362', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"781957534904\",\"userId\":\"jay\"},\"id\":\"43216768362\",\"timestamp\":\"2025-09-11 16:25:48.356\"}', 'completed', '2025-09-11 16:25:48', '2025-09-11 16:25:48');
INSERT INTO `task` VALUES (307, 'jay', 'send_award', '27013790420', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"847007343147\",\"userId\":\"jay\"},\"id\":\"27013790420\",\"timestamp\":\"2025-09-11 16:25:48.558\"}', 'completed', '2025-09-11 16:25:48', '2025-09-11 16:25:49');
INSERT INTO `task` VALUES (308, 'jay', 'send_award', '45601336448', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"333682254849\",\"userId\":\"jay\"},\"id\":\"45601336448\",\"timestamp\":\"2025-09-11 16:25:48.717\"}', 'completed', '2025-09-11 16:25:49', '2025-09-11 16:25:49');
INSERT INTO `task` VALUES (309, 'jay', 'send_award', '52247519360', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"558331798435\",\"userId\":\"jay\"},\"id\":\"52247519360\",\"timestamp\":\"2025-09-11 16:25:48.85\"}', 'completed', '2025-09-11 16:25:49', '2025-09-11 16:25:49');
INSERT INTO `task` VALUES (310, 'jay', 'send_award', '77969749109', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"026648834930\",\"userId\":\"jay\"},\"id\":\"77969749109\",\"timestamp\":\"2025-09-11 16:25:49.053\"}', 'completed', '2025-09-11 16:25:49', '2025-09-11 16:25:49');
INSERT INTO `task` VALUES (311, 'jay', 'send_award', '01521359111', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"743509727445\",\"userId\":\"jay\"},\"id\":\"01521359111\",\"timestamp\":\"2025-09-11 16:25:49.171\"}', 'completed', '2025-09-11 16:25:49', '2025-09-11 16:25:49');
INSERT INTO `task` VALUES (312, 'jay', 'send_award', '48170611366', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"074378267714\",\"userId\":\"jay\"},\"id\":\"48170611366\",\"timestamp\":\"2025-09-11 16:25:49.326\"}', 'completed', '2025-09-11 16:25:49', '2025-09-11 16:25:49');
INSERT INTO `task` VALUES (313, 'jay', 'send_award', '50452994515', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"508985175657\",\"userId\":\"jay\"},\"id\":\"50452994515\",\"timestamp\":\"2025-09-11 16:25:49.501\"}', 'completed', '2025-09-11 16:25:49', '2025-09-11 16:25:50');
INSERT INTO `task` VALUES (314, 'jay', 'send_award', '52124890683', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"549839403269\",\"userId\":\"jay\"},\"id\":\"52124890683\",\"timestamp\":\"2025-09-11 16:25:49.675\"}', 'completed', '2025-09-11 16:25:50', '2025-09-11 16:25:50');
INSERT INTO `task` VALUES (315, 'jay', 'send_award', '11735792079', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"070452517982\",\"userId\":\"jay\"},\"id\":\"11735792079\",\"timestamp\":\"2025-09-11 16:25:49.852\"}', 'completed', '2025-09-11 16:25:50', '2025-09-11 16:25:50');
INSERT INTO `task` VALUES (316, 'jay', 'send_award', '82406157655', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"932629270157\",\"userId\":\"jay\"},\"id\":\"82406157655\",\"timestamp\":\"2025-09-11 16:25:50.006\"}', 'completed', '2025-09-11 16:25:50', '2025-09-11 16:25:50');
INSERT INTO `task` VALUES (317, 'jay', 'send_award', '66630841932', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"831208651493\",\"userId\":\"jay\"},\"id\":\"66630841932\",\"timestamp\":\"2025-09-11 16:25:50.217\"}', 'completed', '2025-09-11 16:25:50', '2025-09-11 16:25:50');
INSERT INTO `task` VALUES (318, 'jay', 'send_award', '18441008810', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"755053385182\",\"userId\":\"jay\"},\"id\":\"18441008810\",\"timestamp\":\"2025-09-11 16:25:50.45\"}', 'completed', '2025-09-11 16:25:50', '2025-09-11 16:25:51');
INSERT INTO `task` VALUES (319, 'jay', 'send_award', '95195396949', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"516887892627\",\"userId\":\"jay\"},\"id\":\"95195396949\",\"timestamp\":\"2025-09-11 16:25:50.566\"}', 'completed', '2025-09-11 16:25:50', '2025-09-11 16:25:51');
INSERT INTO `task` VALUES (320, 'jay', 'send_award', '25100259300', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"479504085878\",\"userId\":\"jay\"},\"id\":\"25100259300\",\"timestamp\":\"2025-09-11 16:25:50.691\"}', 'completed', '2025-09-11 16:25:51', '2025-09-11 16:25:51');
INSERT INTO `task` VALUES (321, 'jay', 'send_award', '94729984851', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"768119144839\",\"userId\":\"jay\"},\"id\":\"94729984851\",\"timestamp\":\"2025-09-11 16:25:50.818\"}', 'completed', '2025-09-11 16:25:51', '2025-09-11 16:25:51');
INSERT INTO `task` VALUES (322, 'jay', 'send_award', '54474377309', '{\"data\":{\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"144414259715\",\"userId\":\"jay\"},\"id\":\"54474377309\",\"timestamp\":\"2025-09-11 16:25:51.022\"}', 'completed', '2025-09-11 16:25:51', '2025-09-11 16:25:51');
INSERT INTO `task` VALUES (323, 'jay', 'send_award', '76252594498', '{\"data\":{\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"773169473027\",\"userId\":\"jay\"},\"id\":\"76252594498\",\"timestamp\":\"2025-09-11 16:25:51.202\"}', 'completed', '2025-09-11 16:25:51', '2025-09-11 16:25:51');
INSERT INTO `task` VALUES (324, 'jay', 'send_award', '44207786127', '{\"data\":{\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"065288892123\",\"userId\":\"jay\"},\"id\":\"44207786127\",\"timestamp\":\"2025-09-11 16:25:51.536\"}', 'completed', '2025-09-11 16:25:51', '2025-09-11 16:25:52');
INSERT INTO `task` VALUES (325, 'jay', 'send_award', '43224420840', '{\"data\":{\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"985109117801\",\"userId\":\"jay\"},\"id\":\"43224420840\",\"timestamp\":\"2025-09-11 16:25:51.702\"}', 'completed', '2025-09-11 16:25:52', '2025-09-11 16:25:52');
INSERT INTO `task` VALUES (326, 'jay', 'send_award', '33997895374', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"783215776478\",\"userId\":\"jay\"},\"id\":\"33997895374\",\"timestamp\":\"2025-09-11 16:25:51.838\"}', 'completed', '2025-09-11 16:25:52', '2025-09-11 16:25:52');
INSERT INTO `task` VALUES (327, 'jay', 'send_award', '88200085855', '{\"data\":{\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"839870176735\",\"userId\":\"jay\"},\"id\":\"88200085855\",\"timestamp\":\"2025-09-11 16:25:51.95\"}', 'completed', '2025-09-11 16:25:52', '2025-09-11 16:25:52');
INSERT INTO `task` VALUES (328, 'jay', 'send_award', '99254837798', '{\"data\":{\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"664037492884\",\"userId\":\"jay\"},\"id\":\"99254837798\",\"timestamp\":\"2025-09-11 16:25:52.116\"}', 'completed', '2025-09-11 16:25:52', '2025-09-11 16:25:52');
INSERT INTO `task` VALUES (329, 'jay', 'send_award', '60806800377', '{\"data\":{\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"586084309178\",\"userId\":\"jay\"},\"id\":\"60806800377\",\"timestamp\":\"2025-09-11 16:25:52.348\"}', 'completed', '2025-09-11 16:25:52', '2025-09-11 16:25:52');
INSERT INTO `task` VALUES (330, 'jay', 'send_award', '83836505062', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"956670727490\",\"userId\":\"jay\"},\"id\":\"83836505062\",\"timestamp\":\"2025-09-11 16:25:52.46\"}', 'completed', '2025-09-11 16:25:52', '2025-09-11 16:25:52');
INSERT INTO `task` VALUES (331, 'jay', 'send_award', '78825071312', '{\"data\":{\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"031398518710\",\"userId\":\"jay\"},\"id\":\"78825071312\",\"timestamp\":\"2025-09-11 16:25:52.586\"}', 'completed', '2025-09-11 16:25:52', '2025-09-11 16:25:53');
INSERT INTO `task` VALUES (332, 'jay', 'send_award', '90908819957', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"915593783833\",\"userId\":\"jay\"},\"id\":\"90908819957\",\"timestamp\":\"2025-09-11 16:25:52.772\"}', 'completed', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `task` VALUES (333, 'jay', 'send_award', '08310114373', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"102342243297\",\"userId\":\"jay\"},\"id\":\"08310114373\",\"timestamp\":\"2025-09-11 16:25:52.943\"}', 'completed', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `task` VALUES (334, 'jay', 'send_award', '30779716969', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"174948679152\",\"userId\":\"jay\"},\"id\":\"30779716969\",\"timestamp\":\"2025-09-11 16:25:53.047\"}', 'completed', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `task` VALUES (335, 'jay', 'send_award', '05704361263', '{\"data\":{\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"455316181452\",\"userId\":\"jay\"},\"id\":\"05704361263\",\"timestamp\":\"2025-09-11 16:25:53.15\"}', 'completed', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `task` VALUES (336, 'jay', 'send_award', '96437612117', '{\"data\":{\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"165163642098\",\"userId\":\"jay\"},\"id\":\"96437612117\",\"timestamp\":\"2025-09-11 16:25:53.287\"}', 'completed', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `task` VALUES (337, 'jay', 'send_award', '97949734351', '{\"data\":{\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"258394248391\",\"userId\":\"jay\"},\"id\":\"97949734351\",\"timestamp\":\"2025-09-11 16:25:53.481\"}', 'completed', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `task` VALUES (338, 'jay', 'send_award', '67186826153', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"514988530785\",\"userId\":\"jay\"},\"id\":\"67186826153\",\"timestamp\":\"2025-09-11 16:25:53.598\"}', 'completed', '2025-09-11 16:25:53', '2025-09-11 16:25:54');
INSERT INTO `task` VALUES (339, 'jay', 'send_award', '72947296961', '{\"data\":{\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"460369076599\",\"userId\":\"jay\"},\"id\":\"72947296961\",\"timestamp\":\"2025-09-11 16:25:53.722\"}', 'completed', '2025-09-11 16:25:54', '2025-09-11 16:25:54');
INSERT INTO `task` VALUES (340, 'jay', 'send_award', '56324016359', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"723454370226\",\"userId\":\"jay\"},\"id\":\"56324016359\",\"timestamp\":\"2025-09-11 16:25:53.839\"}', 'completed', '2025-09-11 16:25:54', '2025-09-11 16:25:54');
INSERT INTO `task` VALUES (341, 'jay', 'send_award', '97231529222', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"902258464786\",\"userId\":\"jay\"},\"id\":\"97231529222\",\"timestamp\":\"2025-09-11 16:25:54.013\"}', 'completed', '2025-09-11 16:25:54', '2025-09-11 16:25:54');
INSERT INTO `task` VALUES (342, 'jay', 'send_award', '23723993674', '{\"data\":{\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"224233858952\",\"userId\":\"jay\"},\"id\":\"23723993674\",\"timestamp\":\"2025-09-11 16:25:54.165\"}', 'completed', '2025-09-11 16:25:54', '2025-09-11 16:25:54');
INSERT INTO `task` VALUES (343, 'jay', 'send_award', '18807486229', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"960866059786\",\"userId\":\"jay\"},\"id\":\"18807486229\",\"timestamp\":\"2025-09-11 16:25:54.362\"}', 'completed', '2025-09-11 16:25:54', '2025-09-11 16:25:54');
INSERT INTO `task` VALUES (344, 'jay', 'send_award', '19648357417', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"361355765275\",\"userId\":\"jay\"},\"id\":\"19648357417\",\"timestamp\":\"2025-09-11 16:25:54.499\"}', 'completed', '2025-09-11 16:25:54', '2025-09-11 16:25:55');
INSERT INTO `task` VALUES (345, 'jay', 'send_award', '82817480371', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"885952284004\",\"userId\":\"jay\"},\"id\":\"82817480371\",\"timestamp\":\"2025-09-11 16:25:54.643\"}', 'completed', '2025-09-11 16:25:55', '2025-09-11 16:25:55');
INSERT INTO `task` VALUES (346, 'jay', 'send_award', '28907919513', '{\"data\":{\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"083192581036\",\"userId\":\"jay\"},\"id\":\"28907919513\",\"timestamp\":\"2025-09-11 16:25:54.781\"}', 'completed', '2025-09-11 16:25:55', '2025-09-11 16:25:55');
INSERT INTO `task` VALUES (347, 'jay', 'send_award', '46661794653', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"971260585952\",\"userId\":\"jay\"},\"id\":\"46661794653\",\"timestamp\":\"2025-09-11 19:57:59.324\"}', 'completed', '2025-09-11 19:57:59', '2025-09-11 19:57:59');
INSERT INTO `task` VALUES (348, 'jay', 'send_award', '55816032680', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"349104647196\",\"userId\":\"jay\"},\"id\":\"55816032680\",\"timestamp\":\"2025-09-11 20:16:06.345\"}', 'completed', '2025-09-11 20:16:06', '2025-09-11 20:16:06');
INSERT INTO `task` VALUES (349, 'fvpki7.keb22', 'send_award', '90389840070', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"152137500941\",\"userId\":\"fvpki7.keb22\"},\"id\":\"90389840070\",\"timestamp\":\"2025-09-11 20:22:02.479\"}', 'completed', '2025-09-11 20:22:02', '2025-09-11 20:22:02');
INSERT INTO `task` VALUES (350, 'fvpki7.keb22', 'send_award', '83943540517', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"605504311002\",\"userId\":\"fvpki7.keb22\"},\"id\":\"83943540517\",\"timestamp\":\"2025-09-11 20:22:09.28\"}', 'completed', '2025-09-11 20:22:09', '2025-09-11 20:22:09');
INSERT INTO `task` VALUES (351, 'fvpki7.keb22', 'send_award', '35295290258', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"450114916627\",\"userId\":\"fvpki7.keb22\"},\"id\":\"35295290258\",\"timestamp\":\"2025-09-11 20:22:14.271\"}', 'completed', '2025-09-11 20:22:14', '2025-09-11 20:22:14');
INSERT INTO `task` VALUES (352, 'jay', 'send_award', '71788665458', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"520284234489\",\"userId\":\"jay\"},\"id\":\"71788665458\",\"timestamp\":\"2025-09-11 20:35:55.789\"}', 'completed', '2025-09-11 20:35:55', '2025-09-11 20:35:55');
INSERT INTO `task` VALUES (353, 'jay', 'send_award', '42609501489', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"584585502041\",\"userId\":\"jay\"},\"id\":\"42609501489\",\"timestamp\":\"2025-09-11 20:37:03.967\"}', 'completed', '2025-09-11 20:37:04', '2025-09-11 20:37:04');
INSERT INTO `task` VALUES (354, 'jay', 'send_award', '20945879744', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"325253046571\",\"userId\":\"jay\"},\"id\":\"20945879744\",\"timestamp\":\"2025-09-11 20:37:04.05\"}', 'completed', '2025-09-11 20:37:04', '2025-09-11 20:37:04');
INSERT INTO `task` VALUES (355, 'jay', 'send_award', '61057500147', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"516850082813\",\"userId\":\"jay\"},\"id\":\"61057500147\",\"timestamp\":\"2025-09-11 20:37:04.109\"}', 'completed', '2025-09-11 20:37:04', '2025-09-11 20:37:04');
INSERT INTO `task` VALUES (356, 'fvpki7.keb22', 'send_award', '48214468644', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"912745270715\",\"userId\":\"fvpki7.keb22\"},\"id\":\"48214468644\",\"timestamp\":\"2025-09-11 20:37:04.789\"}', 'completed', '2025-09-11 20:37:04', '2025-09-11 20:37:04');
INSERT INTO `task` VALUES (357, 'jay', 'send_award', '76777457688', '{\"data\":{\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"034783208182\",\"userId\":\"jay\"},\"id\":\"76777457688\",\"timestamp\":\"2025-09-11 20:37:05.174\"}', 'completed', '2025-09-11 20:37:05', '2025-09-11 20:37:05');
INSERT INTO `task` VALUES (358, 'jay', 'send_award', '11066585507', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"619643666484\",\"userId\":\"jay\"},\"id\":\"11066585507\",\"timestamp\":\"2025-09-11 20:37:05.372\"}', 'completed', '2025-09-11 20:37:05', '2025-09-11 20:37:05');
INSERT INTO `task` VALUES (359, 'jay', 'send_award', '08201099516', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"023739472554\",\"userId\":\"jay\"},\"id\":\"08201099516\",\"timestamp\":\"2025-09-11 20:37:05.472\"}', 'completed', '2025-09-11 20:37:05', '2025-09-11 20:37:05');
INSERT INTO `task` VALUES (360, 'jay', 'send_award', '58899139738', '{\"data\":{\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"469960539768\",\"userId\":\"jay\"},\"id\":\"58899139738\",\"timestamp\":\"2025-09-11 20:37:05.619\"}', 'completed', '2025-09-11 20:37:05', '2025-09-11 20:37:05');
INSERT INTO `task` VALUES (361, 'ixjlj1_fgs', 'send_award', '70739217656', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"485358948528\",\"userId\":\"ixjlj1_fgs\"},\"id\":\"70739217656\",\"timestamp\":\"2025-09-11 20:37:05.655\"}', 'completed', '2025-09-11 20:37:05', '2025-09-11 20:37:05');
INSERT INTO `task` VALUES (362, 'jay', 'send_award', '24989367781', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"721474107557\",\"userId\":\"jay\"},\"id\":\"24989367781\",\"timestamp\":\"2025-09-11 20:37:05.771\"}', 'completed', '2025-09-11 20:37:05', '2025-09-11 20:37:05');
INSERT INTO `task` VALUES (363, 'jay', 'send_award', '32742641937', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"510549235023\",\"userId\":\"jay\"},\"id\":\"32742641937\",\"timestamp\":\"2025-09-11 20:37:05.931\"}', 'completed', '2025-09-11 20:37:06', '2025-09-11 20:37:06');
INSERT INTO `task` VALUES (364, 'jay', 'send_award', '22664404136', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"693555010533\",\"userId\":\"jay\"},\"id\":\"22664404136\",\"timestamp\":\"2025-09-11 20:37:06.045\"}', 'completed', '2025-09-11 20:37:06', '2025-09-11 20:37:06');
INSERT INTO `task` VALUES (365, 'fvpki7.keb22', 'send_award', '02732161599', '{\"data\":{\"awardConfig\":\"\",\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"749956198335\",\"userId\":\"fvpki7.keb22\"},\"id\":\"02732161599\",\"timestamp\":\"2025-09-11 20:37:06.228\"}', 'completed', '2025-09-11 20:37:06', '2025-09-11 20:37:06');
INSERT INTO `task` VALUES (366, 'fvpki7.keb22', 'send_award', '45790532100', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"040211232269\",\"userId\":\"fvpki7.keb22\"},\"id\":\"45790532100\",\"timestamp\":\"2025-09-11 20:37:06.448\"}', 'completed', '2025-09-11 20:37:06', '2025-09-11 20:37:06');
INSERT INTO `task` VALUES (367, 'v9bmdm98', 'send_award', '37075612744', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"736666802256\",\"userId\":\"v9bmdm98\"},\"id\":\"37075612744\",\"timestamp\":\"2025-09-11 20:37:06.452\"}', 'completed', '2025-09-11 20:37:06', '2025-09-11 20:37:06');
INSERT INTO `task` VALUES (368, 'l7nrqi39', 'send_award', '86300572542', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"001082865194\",\"userId\":\"l7nrqi39\"},\"id\":\"86300572542\",\"timestamp\":\"2025-09-11 20:37:06.98\"}', 'completed', '2025-09-11 20:37:07', '2025-09-11 20:37:07');
INSERT INTO `task` VALUES (369, 'fvpki7.keb22', 'send_award', '49558292747', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"245170135024\",\"userId\":\"fvpki7.keb22\"},\"id\":\"49558292747\",\"timestamp\":\"2025-09-11 20:37:07.54\"}', 'completed', '2025-09-11 20:37:07', '2025-09-11 20:37:07');
INSERT INTO `task` VALUES (370, 'ixjlj1_fgs', 'send_award', '95279950836', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"548043642799\",\"userId\":\"ixjlj1_fgs\"},\"id\":\"95279950836\",\"timestamp\":\"2025-09-11 20:37:07.829\"}', 'completed', '2025-09-11 20:37:07', '2025-09-11 20:37:08');
INSERT INTO `task` VALUES (371, 'ixjlj1_fgs', 'send_award', '32999068926', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"532429947610\",\"userId\":\"ixjlj1_fgs\"},\"id\":\"32999068926\",\"timestamp\":\"2025-09-11 20:37:07.962\"}', 'completed', '2025-09-11 20:37:08', '2025-09-11 20:37:08');
INSERT INTO `task` VALUES (372, 'tbfv9r_l7q84', 'send_award', '57783664863', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"417206744050\",\"userId\":\"tbfv9r_l7q84\"},\"id\":\"57783664863\",\"timestamp\":\"2025-09-11 20:37:08.072\"}', 'completed', '2025-09-11 20:37:08', '2025-09-11 20:37:08');
INSERT INTO `task` VALUES (373, 'ixjlj1_fgs', 'send_award', '39281609945', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"524147420844\",\"userId\":\"ixjlj1_fgs\"},\"id\":\"39281609945\",\"timestamp\":\"2025-09-11 20:37:08.131\"}', 'completed', '2025-09-11 20:37:08', '2025-09-11 20:37:08');
INSERT INTO `task` VALUES (374, 'ixjlj1_fgs', 'send_award', '90079454259', '{\"data\":{\"awardConfig\":\"\",\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"138453371004\",\"userId\":\"ixjlj1_fgs\"},\"id\":\"90079454259\",\"timestamp\":\"2025-09-11 20:37:08.2\"}', 'completed', '2025-09-11 20:37:08', '2025-09-11 20:37:08');
INSERT INTO `task` VALUES (375, 'ixjlj1_fgs', 'send_award', '58630298164', '{\"data\":{\"awardConfig\":\"\",\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"112183785106\",\"userId\":\"ixjlj1_fgs\"},\"id\":\"58630298164\",\"timestamp\":\"2025-09-11 20:37:08.331\"}', 'completed', '2025-09-11 20:37:08', '2025-09-11 20:37:08');
INSERT INTO `task` VALUES (376, 'jwupu5.gjv', 'send_award', '70926575249', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"133003370970\",\"userId\":\"jwupu5.gjv\"},\"id\":\"70926575249\",\"timestamp\":\"2025-09-11 20:37:08.603\"}', 'completed', '2025-09-11 20:37:08', '2025-09-11 20:37:08');
INSERT INTO `task` VALUES (377, 'v9bmdm98', 'send_award', '04694360850', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"110775777610\",\"userId\":\"v9bmdm98\"},\"id\":\"04694360850\",\"timestamp\":\"2025-09-11 20:37:08.996\"}', 'completed', '2025-09-11 20:37:09', '2025-09-11 20:37:09');
INSERT INTO `task` VALUES (378, 'jay', 'send_award', '50449711589', '{\"data\":{\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"870993828346\",\"userId\":\"jay\"},\"id\":\"50449711589\",\"timestamp\":\"2025-09-11 20:40:05.051\"}', 'completed', '2025-09-11 20:40:05', '2025-09-11 20:40:05');
INSERT INTO `task` VALUES (379, 'jay', 'send_award', '69969241635', '{\"data\":{\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"298068288177\",\"userId\":\"jay\"},\"id\":\"69969241635\",\"timestamp\":\"2025-09-11 20:40:32.379\"}', 'completed', '2025-09-11 20:40:32', '2025-09-11 20:40:32');
INSERT INTO `task` VALUES (380, 'fvpki7.keb22', 'send_award', '92009213790', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"892818350905\",\"userId\":\"fvpki7.keb22\"},\"id\":\"92009213790\",\"timestamp\":\"2025-09-11 20:40:33.108\"}', 'completed', '2025-09-11 20:40:33', '2025-09-11 20:40:33');
INSERT INTO `task` VALUES (381, 'ixjlj1_fgs', 'send_award', '66130157207', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"499395995020\",\"userId\":\"ixjlj1_fgs\"},\"id\":\"66130157207\",\"timestamp\":\"2025-09-11 20:40:33.829\"}', 'completed', '2025-09-11 20:40:33', '2025-09-11 20:40:33');
INSERT INTO `task` VALUES (382, 'v9bmdm98', 'send_award', '00093525870', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"356166292511\",\"userId\":\"v9bmdm98\"},\"id\":\"00093525870\",\"timestamp\":\"2025-09-11 20:40:34.521\"}', 'completed', '2025-09-11 20:40:34', '2025-09-11 20:40:34');
INSERT INTO `task` VALUES (383, 'l7nrqi39', 'send_award', '29682910015', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"703591723821\",\"userId\":\"l7nrqi39\"},\"id\":\"29682910015\",\"timestamp\":\"2025-09-11 20:40:35.206\"}', 'completed', '2025-09-11 20:40:35', '2025-09-11 20:40:35');
INSERT INTO `task` VALUES (384, 'tbfv9r_l7q84', 'send_award', '90999662956', '{\"data\":{\"awardConfig\":\"\",\"awardId\":106,\"awardTitle\":\"轻奢办公椅\",\"orderId\":\"225304149056\",\"userId\":\"tbfv9r_l7q84\"},\"id\":\"90999662956\",\"timestamp\":\"2025-09-11 20:40:35.888\"}', 'completed', '2025-09-11 20:40:35', '2025-09-11 20:40:36');
INSERT INTO `task` VALUES (385, 'jwupu5.gjv', 'send_award', '54872060405', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"752808039912\",\"userId\":\"jwupu5.gjv\"},\"id\":\"54872060405\",\"timestamp\":\"2025-09-11 20:40:36.61\"}', 'completed', '2025-09-11 20:40:36', '2025-09-11 20:40:36');
INSERT INTO `task` VALUES (386, 'lh7lnf.iul17', 'send_award', '87306621681', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"756798941877\",\"userId\":\"lh7lnf.iul17\"},\"id\":\"87306621681\",\"timestamp\":\"2025-09-11 20:40:37.288\"}', 'completed', '2025-09-11 20:40:37', '2025-09-11 20:40:37');
INSERT INTO `task` VALUES (387, 'p72k3d55', 'send_award', '15024666406', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"249672253957\",\"userId\":\"p72k3d55\"},\"id\":\"15024666406\",\"timestamp\":\"2025-09-11 20:40:38\"}', 'completed', '2025-09-11 20:40:38', '2025-09-11 20:40:38');
INSERT INTO `task` VALUES (388, 'izhfoz52', 'send_award', '84123533927', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"256087726548\",\"userId\":\"izhfoz52\"},\"id\":\"84123533927\",\"timestamp\":\"2025-09-11 20:40:38.72\"}', 'completed', '2025-09-11 20:40:38', '2025-09-11 20:40:38');
INSERT INTO `task` VALUES (389, 'jlbi46_jew', 'send_award', '57123343686', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"178433616575\",\"userId\":\"jlbi46_jew\"},\"id\":\"57123343686\",\"timestamp\":\"2025-09-11 20:40:39.376\"}', 'completed', '2025-09-11 20:40:39', '2025-09-11 20:40:39');
INSERT INTO `task` VALUES (390, 'vdak5x51', 'send_award', '89212923551', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"221950821442\",\"userId\":\"vdak5x51\"},\"id\":\"89212923551\",\"timestamp\":\"2025-09-11 20:40:40.086\"}', 'completed', '2025-09-11 20:40:40', '2025-09-11 20:40:40');
INSERT INTO `task` VALUES (391, 'k1zsbt76', 'send_award', '10458046099', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"756591068309\",\"userId\":\"k1zsbt76\"},\"id\":\"10458046099\",\"timestamp\":\"2025-09-11 20:40:40.793\"}', 'completed', '2025-09-11 20:40:40', '2025-09-11 20:40:41');
INSERT INTO `task` VALUES (392, 'v9blnf12', 'send_award', '77309682850', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"926235935225\",\"userId\":\"v9blnf12\"},\"id\":\"77309682850\",\"timestamp\":\"2025-09-11 20:40:41.569\"}', 'completed', '2025-09-11 20:40:41', '2025-09-11 20:40:41');
INSERT INTO `task` VALUES (393, 'ggqryd_skd44', 'send_award', '87592838868', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"202461115156\",\"userId\":\"ggqryd_skd44\"},\"id\":\"87592838868\",\"timestamp\":\"2025-09-11 20:40:42.253\"}', 'completed', '2025-09-11 20:40:42', '2025-09-11 20:40:42');
INSERT INTO `task` VALUES (394, 'fsekfk.jh485', 'send_award', '38202062582', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"777944065189\",\"userId\":\"fsekfk.jh485\"},\"id\":\"38202062582\",\"timestamp\":\"2025-09-11 20:40:42.872\"}', 'completed', '2025-09-11 20:40:42', '2025-09-11 20:40:43');
INSERT INTO `task` VALUES (395, 'k8qkfk86', 'send_award', '57190649895', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"716151311315\",\"userId\":\"k8qkfk86\"},\"id\":\"57190649895\",\"timestamp\":\"2025-09-11 20:40:43.522\"}', 'completed', '2025-09-11 20:40:43', '2025-09-11 20:40:43');
INSERT INTO `task` VALUES (396, 'pd6fvp_kd762', 'send_award', '49099582821', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"690852255757\",\"userId\":\"pd6fvp_kd762\"},\"id\":\"49099582821\",\"timestamp\":\"2025-09-11 20:40:44.255\"}', 'completed', '2025-09-11 20:40:44', '2025-09-11 20:40:44');
INSERT INTO `task` VALUES (397, 'irescp24', 'send_award', '50305969351', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"262982027709\",\"userId\":\"irescp24\"},\"id\":\"50305969351\",\"timestamp\":\"2025-09-11 20:40:44.946\"}', 'completed', '2025-09-11 20:40:45', '2025-09-11 20:40:45');
INSERT INTO `task` VALUES (398, 'lm1_p1k', 'send_award', '27239052194', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"223908492570\",\"userId\":\"lm1_p1k\"},\"id\":\"27239052194\",\"timestamp\":\"2025-09-11 20:40:45.632\"}', 'completed', '2025-09-11 20:40:45', '2025-09-11 20:40:45');
INSERT INTO `task` VALUES (399, 'tgmo3v_tg9', 'send_award', '57063521985', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"670748640780\",\"userId\":\"tgmo3v_tg9\"},\"id\":\"57063521985\",\"timestamp\":\"2025-09-11 20:40:46.341\"}', 'completed', '2025-09-11 20:40:46', '2025-09-11 20:40:46');
INSERT INTO `task` VALUES (400, 'sf0ppm47', 'send_award', '94243835412', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"871712532191\",\"userId\":\"sf0ppm47\"},\"id\":\"94243835412\",\"timestamp\":\"2025-09-11 20:40:47.042\"}', 'completed', '2025-09-11 20:40:47', '2025-09-11 20:40:47');
INSERT INTO `task` VALUES (401, 'pgknz9.q5795', 'send_award', '40472247577', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"084473582561\",\"userId\":\"pgknz9.q5795\"},\"id\":\"40472247577\",\"timestamp\":\"2025-09-11 20:40:47.72\"}', 'completed', '2025-09-11 20:40:47', '2025-09-11 20:40:47');
INSERT INTO `task` VALUES (402, 'gs2m5650', 'send_award', '13395758333', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"819518130540\",\"userId\":\"gs2m5650\"},\"id\":\"13395758333\",\"timestamp\":\"2025-09-11 20:40:48.372\"}', 'completed', '2025-09-11 20:40:48', '2025-09-11 20:40:48');
INSERT INTO `task` VALUES (403, 'jzzgm341', 'send_award', '37041386673', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"966825698801\",\"userId\":\"jzzgm341\"},\"id\":\"37041386673\",\"timestamp\":\"2025-09-11 20:40:49.017\"}', 'completed', '2025-09-11 20:40:49', '2025-09-11 20:40:49');
INSERT INTO `task` VALUES (404, 'k8qscp_glx72', 'send_award', '05118576626', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"972070713546\",\"userId\":\"k8qscp_glx72\"},\"id\":\"05118576626\",\"timestamp\":\"2025-09-11 20:40:49.674\"}', 'completed', '2025-09-11 20:40:49', '2025-09-11 20:40:49');
INSERT INTO `task` VALUES (405, 'i2xmy224', 'send_award', '86260480699', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"713708837505\",\"userId\":\"i2xmy224\"},\"id\":\"86260480699\",\"timestamp\":\"2025-09-11 20:40:50.37\"}', 'completed', '2025-09-11 20:40:50', '2025-09-11 20:40:50');
INSERT INTO `task` VALUES (406, 'nlrlqi_j7y', 'send_award', '86588795269', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"313538968950\",\"userId\":\"nlrlqi_j7y\"},\"id\":\"86588795269\",\"timestamp\":\"2025-09-11 20:40:51.019\"}', 'completed', '2025-09-11 20:40:51', '2025-09-11 20:40:51');
INSERT INTO `task` VALUES (407, 'ryilj149', 'send_award', '79537073250', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"148928701524\",\"userId\":\"ryilj149\"},\"id\":\"79537073250\",\"timestamp\":\"2025-09-11 20:40:51.692\"}', 'completed', '2025-09-11 20:40:51', '2025-09-11 20:40:51');
INSERT INTO `task` VALUES (408, 'mzitue_ol733', 'send_award', '47503968478', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"878501532647\",\"userId\":\"mzitue_ol733\"},\"id\":\"47503968478\",\"timestamp\":\"2025-09-11 20:40:52.344\"}', 'completed', '2025-09-11 20:40:52', '2025-09-11 20:40:52');
INSERT INTO `task` VALUES (409, 'ixjlj1_tqo', 'send_award', '79334832908', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"871633686147\",\"userId\":\"ixjlj1_tqo\"},\"id\":\"79334832908\",\"timestamp\":\"2025-09-11 20:40:53.024\"}', 'completed', '2025-09-11 20:40:53', '2025-09-11 20:40:53');
INSERT INTO `task` VALUES (410, 'lm1i2v83', 'send_award', '00207436787', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"618271231279\",\"userId\":\"lm1i2v83\"},\"id\":\"00207436787\",\"timestamp\":\"2025-09-11 20:40:53.707\"}', 'completed', '2025-09-11 20:40:53', '2025-09-11 20:40:53');
INSERT INTO `task` VALUES (411, 'ixjit6.laj61', 'send_award', '51197870710', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"108813262747\",\"userId\":\"ixjit6.laj61\"},\"id\":\"51197870710\",\"timestamp\":\"2025-09-11 20:40:54.427\"}', 'completed', '2025-09-11 20:40:54', '2025-09-11 20:40:54');
INSERT INTO `task` VALUES (412, 'm97k5q.ny968', 'send_award', '61103076673', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"322136064600\",\"userId\":\"m97k5q.ny968\"},\"id\":\"61103076673\",\"timestamp\":\"2025-09-11 20:40:55.06\"}', 'completed', '2025-09-11 20:40:55', '2025-09-11 20:40:55');
INSERT INTO `task` VALUES (413, 'ui8_n1l68', 'send_award', '49281290953', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"063523150482\",\"userId\":\"ui8_n1l68\"},\"id\":\"49281290953\",\"timestamp\":\"2025-09-11 20:40:55.746\"}', 'completed', '2025-09-11 20:40:55', '2025-09-11 20:40:55');
INSERT INTO `task` VALUES (414, 'tt6kfy43', 'send_award', '63704807897', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"617818152639\",\"userId\":\"tt6kfy43\"},\"id\":\"63704807897\",\"timestamp\":\"2025-09-11 20:40:56.453\"}', 'completed', '2025-09-11 20:40:56', '2025-09-11 20:40:56');
INSERT INTO `task` VALUES (415, 'k1z74', 'send_award', '27438577455', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"017668200517\",\"userId\":\"k1z74\"},\"id\":\"27438577455\",\"timestamp\":\"2025-09-11 20:40:57.128\"}', 'completed', '2025-09-11 20:40:57', '2025-09-11 20:40:57');
INSERT INTO `task` VALUES (416, 'oq3kgn38', 'send_award', '75786999830', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"445209923164\",\"userId\":\"oq3kgn38\"},\"id\":\"75786999830\",\"timestamp\":\"2025-09-11 20:40:57.808\"}', 'completed', '2025-09-11 20:40:57', '2025-09-11 20:40:57');
INSERT INTO `task` VALUES (417, 'kgs_man44', 'send_award', '88107580857', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"651754461929\",\"userId\":\"kgs_man44\"},\"id\":\"88107580857\",\"timestamp\":\"2025-09-11 20:40:58.429\"}', 'completed', '2025-09-11 20:40:58', '2025-09-11 20:40:58');
INSERT INTO `task` VALUES (418, 'i1csbt_gqo91', 'send_award', '10724985467', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"650808405297\",\"userId\":\"i1csbt_gqo91\"},\"id\":\"10724985467\",\"timestamp\":\"2025-09-11 20:40:59.105\"}', 'completed', '2025-09-11 20:40:59', '2025-09-11 20:40:59');
INSERT INTO `task` VALUES (419, 'ts5hvb.skx', 'send_award', '45148562276', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"531965810993\",\"userId\":\"ts5hvb.skx\"},\"id\":\"45148562276\",\"timestamp\":\"2025-09-11 20:40:59.746\"}', 'completed', '2025-09-11 20:40:59', '2025-09-11 20:40:59');
INSERT INTO `task` VALUES (420, 'm8eiuk_fl0', 'send_award', '67935595976', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"140757662837\",\"userId\":\"m8eiuk_fl0\"},\"id\":\"67935595976\",\"timestamp\":\"2025-09-11 20:41:00.419\"}', 'completed', '2025-09-11 20:41:00', '2025-09-11 20:41:00');
INSERT INTO `task` VALUES (421, 'k8qkfk.sks1', 'send_award', '43490114024', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"161973468187\",\"userId\":\"k8qkfk.sks1\"},\"id\":\"43490114024\",\"timestamp\":\"2025-09-11 20:41:01.126\"}', 'completed', '2025-09-11 20:41:01', '2025-09-11 20:41:01');
INSERT INTO `task` VALUES (422, 'ui865', 'send_award', '76659925945', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"807771818632\",\"userId\":\"ui865\"},\"id\":\"76659925945\",\"timestamp\":\"2025-09-11 20:41:01.813\"}', 'completed', '2025-09-11 20:41:01', '2025-09-11 20:41:02');
INSERT INTO `task` VALUES (423, 'i2xmy2_fj6', 'send_award', '22194178434', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"729341864240\",\"userId\":\"i2xmy2_fj6\"},\"id\":\"22194178434\",\"timestamp\":\"2025-09-11 20:41:02.637\"}', 'completed', '2025-09-11 20:41:02', '2025-09-11 20:41:02');
INSERT INTO `task` VALUES (424, 'izhpd6_jwt19', 'send_award', '87672682814', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"514193364784\",\"userId\":\"izhpd6_jwt19\"},\"id\":\"87672682814\",\"timestamp\":\"2025-09-11 20:41:03.335\"}', 'completed', '2025-09-11 20:41:03', '2025-09-11 20:41:03');
INSERT INTO `task` VALUES (425, 'tw6my286', 'send_award', '95633623643', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"458886356058\",\"userId\":\"tw6my286\"},\"id\":\"95633623643\",\"timestamp\":\"2025-09-11 20:41:03.986\"}', 'completed', '2025-09-11 20:41:04', '2025-09-11 20:41:04');
INSERT INTO `task` VALUES (426, 'pa8kfk35', 'send_award', '82839879243', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"278771929782\",\"userId\":\"pa8kfk35\"},\"id\":\"82839879243\",\"timestamp\":\"2025-09-11 20:41:04.674\"}', 'completed', '2025-09-11 20:41:04', '2025-09-11 20:41:04');
INSERT INTO `task` VALUES (427, 'tt6kfy.i809', 'send_award', '03675786427', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"501392725475\",\"userId\":\"tt6kfy.i809\"},\"id\":\"03675786427\",\"timestamp\":\"2025-09-11 20:41:05.344\"}', 'completed', '2025-09-11 20:41:05', '2025-09-11 20:41:05');
INSERT INTO `task` VALUES (428, 'i1cvdk88', 'send_award', '05755384491', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"997486405389\",\"userId\":\"i1cvdk88\"},\"id\":\"05755384491\",\"timestamp\":\"2025-09-11 20:41:06.029\"}', 'completed', '2025-09-11 20:41:06', '2025-09-11 20:41:06');
INSERT INTO `task` VALUES (429, 'mawfmn82', 'send_award', '05377895280', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"892744627134\",\"userId\":\"mawfmn82\"},\"id\":\"05377895280\",\"timestamp\":\"2025-09-11 20:41:06.667\"}', 'completed', '2025-09-11 20:41:06', '2025-09-11 20:41:06');
INSERT INTO `task` VALUES (430, 'fmnrmi_ff1', 'send_award', '04126892265', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"881604419574\",\"userId\":\"fmnrmi_ff1\"},\"id\":\"04126892265\",\"timestamp\":\"2025-09-11 20:41:07.335\"}', 'completed', '2025-09-11 20:41:07', '2025-09-11 20:41:07');
INSERT INTO `task` VALUES (431, 'me4nzu_ph932', 'send_award', '01981454777', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"649329124724\",\"userId\":\"me4nzu_ph932\"},\"id\":\"01981454777\",\"timestamp\":\"2025-09-11 20:41:08.035\"}', 'completed', '2025-09-11 20:41:08', '2025-09-11 20:41:08');
INSERT INTO `task` VALUES (432, 'tbfv9r_ssq', 'send_award', '87463249074', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"806787329592\",\"userId\":\"tbfv9r_ssq\"},\"id\":\"87463249074\",\"timestamp\":\"2025-09-11 20:41:08.684\"}', 'completed', '2025-09-11 20:41:08', '2025-09-11 20:41:08');
INSERT INTO `task` VALUES (433, 'v9rlnf.gbl49', 'send_award', '36194912872', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"695698496764\",\"userId\":\"v9rlnf.gbl49\"},\"id\":\"36194912872\",\"timestamp\":\"2025-09-11 20:41:09.354\"}', 'completed', '2025-09-11 20:41:09', '2025-09-11 20:41:09');
INSERT INTO `task` VALUES (434, 'k5mhm1_ky2', 'send_award', '11065142571', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"478297588215\",\"userId\":\"k5mhm1_ky2\"},\"id\":\"11065142571\",\"timestamp\":\"2025-09-11 20:41:10.001\"}', 'completed', '2025-09-11 20:41:10', '2025-09-11 20:41:10');
INSERT INTO `task` VALUES (435, 'k5qkfk.qap8', 'send_award', '01291413863', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"903258584118\",\"userId\":\"k5qkfk.qap8\"},\"id\":\"01291413863\",\"timestamp\":\"2025-09-11 20:41:10.638\"}', 'completed', '2025-09-11 20:41:10', '2025-09-11 20:41:10');
INSERT INTO `task` VALUES (436, 'pu9ggq.gc3', 'send_award', '04969395874', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"698154511241\",\"userId\":\"pu9ggq.gc3\"},\"id\":\"04969395874\",\"timestamp\":\"2025-09-11 20:41:11.319\"}', 'completed', '2025-09-11 20:41:11', '2025-09-11 20:41:11');
INSERT INTO `task` VALUES (437, 'rmftt492', 'send_award', '31307141347', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"392430616332\",\"userId\":\"rmftt492\"},\"id\":\"31307141347\",\"timestamp\":\"2025-09-11 20:41:12.033\"}', 'completed', '2025-09-11 20:41:12', '2025-09-11 20:41:12');
INSERT INTO `task` VALUES (438, 'tt6kfy.lly', 'send_award', '90891342579', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"583573290486\",\"userId\":\"tt6kfy.lly\"},\"id\":\"90891342579\",\"timestamp\":\"2025-09-11 20:41:12.705\"}', 'completed', '2025-09-11 20:41:12', '2025-09-11 20:41:12');
INSERT INTO `task` VALUES (439, 'ui859', 'send_award', '45766917014', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"495875507119\",\"userId\":\"ui859\"},\"id\":\"45766917014\",\"timestamp\":\"2025-09-11 20:41:13.4\"}', 'completed', '2025-09-11 20:41:13', '2025-09-11 20:41:13');
INSERT INTO `task` VALUES (440, 'v9ru724', 'send_award', '15321416901', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"789400922936\",\"userId\":\"v9ru724\"},\"id\":\"15321416901\",\"timestamp\":\"2025-09-11 20:41:14.112\"}', 'completed', '2025-09-11 20:41:14', '2025-09-11 20:41:14');
INSERT INTO `task` VALUES (441, 'hm1nuy43', 'send_award', '89319993715', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"530579532664\",\"userId\":\"hm1nuy43\"},\"id\":\"89319993715\",\"timestamp\":\"2025-09-11 20:41:14.935\"}', 'completed', '2025-09-11 20:41:15', '2025-09-11 20:41:15');
INSERT INTO `task` VALUES (442, 'nlrlqi66', 'send_award', '52721112330', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"771204990342\",\"userId\":\"nlrlqi66\"},\"id\":\"52721112330\",\"timestamp\":\"2025-09-11 20:41:15.754\"}', 'completed', '2025-09-11 20:41:15', '2025-09-11 20:41:15');
INSERT INTO `task` VALUES (443, 'fevfoz.gne23', 'send_award', '02716642934', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"617071776299\",\"userId\":\"fevfoz.gne23\"},\"id\":\"02716642934\",\"timestamp\":\"2025-09-11 20:41:16.535\"}', 'completed', '2025-09-11 20:41:16', '2025-09-11 20:41:16');
INSERT INTO `task` VALUES (444, 'k8qi48.pip39', 'send_award', '89146801365', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"695425801466\",\"userId\":\"k8qi48.pip39\"},\"id\":\"89146801365\",\"timestamp\":\"2025-09-11 20:41:17.351\"}', 'completed', '2025-09-11 20:41:17', '2025-09-11 20:41:17');
INSERT INTO `task` VALUES (445, 'mzitue.m2hgkw', 'send_award', '15483585098', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"206722639797\",\"userId\":\"mzitue.m2hgkw\"},\"id\":\"15483585098\",\"timestamp\":\"2025-09-11 20:41:18.171\"}', 'completed', '2025-09-11 20:41:18', '2025-09-11 20:41:18');
INSERT INTO `task` VALUES (446, 'izhlq4.lpvfim58', 'send_award', '56930765159', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"907577096403\",\"userId\":\"izhlq4.lpvfim58\"},\"id\":\"56930765159\",\"timestamp\":\"2025-09-11 20:41:18.944\"}', 'completed', '2025-09-11 20:41:19', '2025-09-11 20:41:19');
INSERT INTO `task` VALUES (447, 'lh7lnf96', 'send_award', '16693645956', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"826087427643\",\"userId\":\"lh7lnf96\"},\"id\":\"16693645956\",\"timestamp\":\"2025-09-11 20:41:19.77\"}', 'completed', '2025-09-11 20:41:19', '2025-09-11 20:41:19');
INSERT INTO `task` VALUES (448, 'fh5q31.ljf', 'send_award', '43654555216', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"860452415786\",\"userId\":\"fh5q31.ljf\"},\"id\":\"43654555216\",\"timestamp\":\"2025-09-11 20:41:20.531\"}', 'completed', '2025-09-11 20:41:20', '2025-09-11 20:41:20');
INSERT INTO `task` VALUES (449, 'gs2k34_ny930', 'send_award', '76495024325', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"615093648577\",\"userId\":\"gs2k34_ny930\"},\"id\":\"76495024325\",\"timestamp\":\"2025-09-11 20:41:21.336\"}', 'completed', '2025-09-11 20:41:21', '2025-09-11 20:41:21');
INSERT INTO `task` VALUES (450, 'mawtue37', 'send_award', '68582740344', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"892261763789\",\"userId\":\"mawtue37\"},\"id\":\"68582740344\",\"timestamp\":\"2025-09-11 20:41:22.122\"}', 'completed', '2025-09-11 20:41:22', '2025-09-11 20:41:22');
INSERT INTO `task` VALUES (451, 'jwupu524', 'send_award', '49301158376', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"249155116559\",\"userId\":\"jwupu524\"},\"id\":\"49301158376\",\"timestamp\":\"2025-09-11 20:41:22.98\"}', 'completed', '2025-09-11 20:41:23', '2025-09-11 20:41:23');
INSERT INTO `task` VALUES (452, 'pu9ggq_h5q81', 'send_award', '40463786775', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"973860844310\",\"userId\":\"pu9ggq_h5q81\"},\"id\":\"40463786775\",\"timestamp\":\"2025-09-11 20:41:23.853\"}', 'completed', '2025-09-11 20:41:23', '2025-09-11 20:41:24');
INSERT INTO `task` VALUES (453, 'hm1nuy_frz89', 'send_award', '69721338653', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"730843364839\",\"userId\":\"hm1nuy_frz89\"},\"id\":\"69721338653\",\"timestamp\":\"2025-09-11 20:41:24.705\"}', 'completed', '2025-09-11 20:41:24', '2025-09-11 20:41:24');
INSERT INTO `task` VALUES (454, 'tgmo3v.tfa99', 'send_award', '61151525512', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"482088172491\",\"userId\":\"tgmo3v.tfa99\"},\"id\":\"61151525512\",\"timestamp\":\"2025-09-11 20:41:25.465\"}', 'completed', '2025-09-11 20:41:25', '2025-09-11 20:41:25');
INSERT INTO `task` VALUES (455, 'k1zu0c.gbf58', 'send_award', '92967496264', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"521343929271\",\"userId\":\"k1zu0c.gbf58\"},\"id\":\"92967496264\",\"timestamp\":\"2025-09-11 20:41:26.316\"}', 'completed', '2025-09-11 20:41:26', '2025-09-11 20:41:26');
INSERT INTO `task` VALUES (456, 'id7mft99', 'send_award', '69183496197', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"854020727435\",\"userId\":\"id7mft99\"},\"id\":\"69183496197\",\"timestamp\":\"2025-09-11 20:41:27.084\"}', 'completed', '2025-09-11 20:41:27', '2025-09-11 20:41:27');
INSERT INTO `task` VALUES (457, 'tbfnuy58', 'send_award', '37367505134', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"488087498598\",\"userId\":\"tbfnuy58\"},\"id\":\"37367505134\",\"timestamp\":\"2025-09-11 20:41:27.848\"}', 'completed', '2025-09-11 20:41:27', '2025-09-11 20:41:28');
INSERT INTO `task` VALUES (458, 'ffxlmf_l9v84', 'send_award', '71675982561', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"882275162643\",\"userId\":\"ffxlmf_l9v84\"},\"id\":\"71675982561\",\"timestamp\":\"2025-09-11 20:41:28.614\"}', 'completed', '2025-09-11 20:41:28', '2025-09-11 20:41:28');
INSERT INTO `task` VALUES (459, 'fmnrmi_ptd', 'send_award', '94205887029', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"084327458304\",\"userId\":\"fmnrmi_ptd\"},\"id\":\"94205887029\",\"timestamp\":\"2025-09-11 20:41:29.417\"}', 'completed', '2025-09-11 20:41:29', '2025-09-11 20:41:29');
INSERT INTO `task` VALUES (460, 'tt4lj1_vix54', 'send_award', '48646676954', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"516407902610\",\"userId\":\"tt4lj1_vix54\"},\"id\":\"48646676954\",\"timestamp\":\"2025-09-11 20:41:30.197\"}', 'completed', '2025-09-11 20:41:30', '2025-09-11 20:41:30');
INSERT INTO `task` VALUES (461, 'irescp12', 'send_award', '44637385756', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"495205823805\",\"userId\":\"irescp12\"},\"id\":\"44637385756\",\"timestamp\":\"2025-09-11 20:41:30.96\"}', 'completed', '2025-09-11 20:41:31', '2025-09-11 20:41:31');
INSERT INTO `task` VALUES (462, 'ttbtw992', 'send_award', '76476113925', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"617734339171\",\"userId\":\"ttbtw992\"},\"id\":\"76476113925\",\"timestamp\":\"2025-09-11 20:41:31.724\"}', 'completed', '2025-09-11 20:41:31', '2025-09-11 20:41:31');
INSERT INTO `task` VALUES (463, 'fozmy264', 'send_award', '63834155641', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"579074268560\",\"userId\":\"fozmy264\"},\"id\":\"63834155641\",\"timestamp\":\"2025-09-11 20:41:32.522\"}', 'completed', '2025-09-11 20:41:32', '2025-09-11 20:41:32');
INSERT INTO `task` VALUES (464, 'rydkjv.g3k46', 'send_award', '86678344356', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"580317468716\",\"userId\":\"rydkjv.g3k46\"},\"id\":\"86678344356\",\"timestamp\":\"2025-09-11 20:41:33.353\"}', 'completed', '2025-09-11 20:41:33', '2025-09-11 20:41:33');
INSERT INTO `task` VALUES (465, 'fgwuih10', 'send_award', '74451911224', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"044003661381\",\"userId\":\"fgwuih10\"},\"id\":\"74451911224\",\"timestamp\":\"2025-09-11 20:41:34.181\"}', 'completed', '2025-09-11 20:41:34', '2025-09-11 20:41:34');
INSERT INTO `task` VALUES (466, 'gs2m5634', 'send_award', '02727189064', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"811524536799\",\"userId\":\"gs2m5634\"},\"id\":\"02727189064\",\"timestamp\":\"2025-09-11 20:41:34.996\"}', 'completed', '2025-09-11 20:41:35', '2025-09-11 20:41:35');
INSERT INTO `task` VALUES (467, 'mawnuy3', 'send_award', '12036632150', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"291889375230\",\"userId\":\"mawnuy3\"},\"id\":\"12036632150\",\"timestamp\":\"2025-09-11 20:41:35.817\"}', 'completed', '2025-09-11 20:41:35', '2025-09-11 20:41:35');
INSERT INTO `task` VALUES (468, 'mawfmn_rlk', 'send_award', '15136626911', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"633936992398\",\"userId\":\"mawfmn_rlk\"},\"id\":\"15136626911\",\"timestamp\":\"2025-09-11 20:41:36.596\"}', 'completed', '2025-09-11 20:41:36', '2025-09-11 20:41:36');
INSERT INTO `task` VALUES (469, 'izhfoz96', 'send_award', '66441587350', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"313042747935\",\"userId\":\"izhfoz96\"},\"id\":\"66441587350\",\"timestamp\":\"2025-09-11 20:41:37.359\"}', 'completed', '2025-09-11 20:41:37', '2025-09-11 20:41:37');
INSERT INTO `task` VALUES (470, 'p72k3d.rvz', 'send_award', '31404792633', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"921974904299\",\"userId\":\"p72k3d.rvz\"},\"id\":\"31404792633\",\"timestamp\":\"2025-09-11 20:41:38.131\"}', 'completed', '2025-09-11 20:41:38', '2025-09-11 20:41:38');
INSERT INTO `task` VALUES (471, 'izhpd6_lxi', 'send_award', '76098149804', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"830180589722\",\"userId\":\"izhpd6_lxi\"},\"id\":\"76098149804\",\"timestamp\":\"2025-09-11 20:41:38.927\"}', 'completed', '2025-09-11 20:41:39', '2025-09-11 20:41:39');
INSERT INTO `task` VALUES (472, 'jzzgm374', 'send_award', '95158291266', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"495836124725\",\"userId\":\"jzzgm374\"},\"id\":\"95158291266\",\"timestamp\":\"2025-09-11 20:41:39.748\"}', 'completed', '2025-09-11 20:41:39', '2025-09-11 20:41:39');
INSERT INTO `task` VALUES (473, 'i1cuji.nev32', 'send_award', '80990877797', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"225776565928\",\"userId\":\"i1cuji.nev32\"},\"id\":\"80990877797\",\"timestamp\":\"2025-09-11 20:41:40.514\"}', 'completed', '2025-09-11 20:41:40', '2025-09-11 20:41:40');
INSERT INTO `task` VALUES (474, 'lj1ljf_g8b10', 'send_award', '99852629756', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"351644873053\",\"userId\":\"lj1ljf_g8b10\"},\"id\":\"99852629756\",\"timestamp\":\"2025-09-11 20:41:41.375\"}', 'completed', '2025-09-11 20:41:41', '2025-09-11 20:41:41');
INSERT INTO `task` VALUES (475, 'hm1nuy.lqd', 'send_award', '56072874583', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"782987856019\",\"userId\":\"hm1nuy.lqd\"},\"id\":\"56072874583\",\"timestamp\":\"2025-09-11 20:41:42.212\"}', 'completed', '2025-09-11 20:41:42', '2025-09-11 20:41:42');
INSERT INTO `task` VALUES (476, 'k1zggq.o34', 'send_award', '41128339722', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"852715642756\",\"userId\":\"k1zggq.o34\"},\"id\":\"41128339722\",\"timestamp\":\"2025-09-11 20:41:42.974\"}', 'completed', '2025-09-11 20:41:43', '2025-09-11 20:41:43');
INSERT INTO `task` VALUES (477, 'v9rmdm_gp82', 'send_award', '98602551974', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"160882356236\",\"userId\":\"v9rmdm_gp82\"},\"id\":\"98602551974\",\"timestamp\":\"2025-09-11 20:41:43.77\"}', 'completed', '2025-09-11 20:41:43', '2025-09-11 20:41:43');
INSERT INTO `task` VALUES (478, 'o97scp88', 'send_award', '86525628962', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"102545732985\",\"userId\":\"o97scp88\"},\"id\":\"86525628962\",\"timestamp\":\"2025-09-11 20:41:44.548\"}', 'completed', '2025-09-11 20:41:44', '2025-09-11 20:41:44');
INSERT INTO `task` VALUES (479, 'izhlq423', 'send_award', '04893230254', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"948359516153\",\"userId\":\"izhlq423\"},\"id\":\"04893230254\",\"timestamp\":\"2025-09-11 20:41:45.393\"}', 'completed', '2025-09-11 20:41:45', '2025-09-11 20:41:45');
INSERT INTO `task` VALUES (480, 'fextfv_hqj', 'send_award', '15804543987', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"940049978778\",\"userId\":\"fextfv_hqj\"},\"id\":\"15804543987\",\"timestamp\":\"2025-09-11 20:41:46.143\"}', 'completed', '2025-09-11 20:41:46', '2025-09-11 20:41:46');
INSERT INTO `task` VALUES (481, 'p1pvgw84', 'send_award', '12933914845', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"581273917437\",\"userId\":\"p1pvgw84\"},\"id\":\"12933914845\",\"timestamp\":\"2025-09-11 20:41:46.903\"}', 'completed', '2025-09-11 20:41:46', '2025-09-11 20:41:47');
INSERT INTO `task` VALUES (482, 'iuk.ptd72', 'send_award', '93973796695', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"219005873316\",\"userId\":\"iuk.ptd72\"},\"id\":\"93973796695\",\"timestamp\":\"2025-09-11 20:41:47.728\"}', 'completed', '2025-09-11 20:41:47', '2025-09-11 20:41:47');
INSERT INTO `task` VALUES (483, 'jwui2v_n5e', 'send_award', '46509974433', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"908380651223\",\"userId\":\"jwui2v_n5e\"},\"id\":\"46509974433\",\"timestamp\":\"2025-09-11 20:41:48.516\"}', 'completed', '2025-09-11 20:41:48', '2025-09-11 20:41:48');
INSERT INTO `task` VALUES (484, 'kutqrx_lal54', 'send_award', '46786545613', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"478453058390\",\"userId\":\"kutqrx_lal54\"},\"id\":\"46786545613\",\"timestamp\":\"2025-09-11 20:41:49.392\"}', 'completed', '2025-09-11 20:41:49', '2025-09-11 20:41:49');
INSERT INTO `task` VALUES (485, 'p72k3d43', 'send_award', '98511084425', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"921294224927\",\"userId\":\"p72k3d43\"},\"id\":\"98511084425\",\"timestamp\":\"2025-09-11 20:41:50.196\"}', 'completed', '2025-09-11 20:41:50', '2025-09-11 20:41:50');
INSERT INTO `task` VALUES (486, 'rmftt4.k28', 'send_award', '17224581153', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"536016673960\",\"userId\":\"rmftt4.k28\"},\"id\":\"17224581153\",\"timestamp\":\"2025-09-11 20:41:51.036\"}', 'completed', '2025-09-11 20:41:51', '2025-09-11 20:41:51');
INSERT INTO `task` VALUES (487, 'i1cvdk31', 'send_award', '38691464640', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"562073825721\",\"userId\":\"i1cvdk31\"},\"id\":\"38691464640\",\"timestamp\":\"2025-09-11 20:41:51.808\"}', 'completed', '2025-09-11 20:41:51', '2025-09-11 20:41:51');
INSERT INTO `task` VALUES (488, 'k8qscp97', 'send_award', '35071540599', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"705400855619\",\"userId\":\"k8qscp97\"},\"id\":\"35071540599\",\"timestamp\":\"2025-09-11 20:41:52.552\"}', 'completed', '2025-09-11 20:41:52', '2025-09-11 20:41:52');
INSERT INTO `task` VALUES (489, 'ixjlj1_kk17', 'send_award', '43640840047', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"301912636493\",\"userId\":\"ixjlj1_kk17\"},\"id\":\"43640840047\",\"timestamp\":\"2025-09-11 20:41:53.354\"}', 'completed', '2025-09-11 20:41:53', '2025-09-11 20:41:53');
INSERT INTO `task` VALUES (490, 'k5mmcx_ny262', 'send_award', '87421685786', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"893159250084\",\"userId\":\"k5mmcx_ny262\"},\"id\":\"87421685786\",\"timestamp\":\"2025-09-11 20:41:54.163\"}', 'completed', '2025-09-11 20:41:54', '2025-09-11 20:41:54');
INSERT INTO `task` VALUES (491, 'k18to3.lg016', 'send_award', '12603060915', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"040552304849\",\"userId\":\"k18to3.lg016\"},\"id\":\"12603060915\",\"timestamp\":\"2025-09-11 20:41:54.99\"}', 'completed', '2025-09-11 20:41:55', '2025-09-11 20:41:55');
INSERT INTO `task` VALUES (492, 'p72k3d.kjv', 'send_award', '43664211088', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"048535936908\",\"userId\":\"p72k3d.kjv\"},\"id\":\"43664211088\",\"timestamp\":\"2025-09-11 20:41:55.796\"}', 'completed', '2025-09-11 20:41:55', '2025-09-11 20:41:55');
INSERT INTO `task` VALUES (493, 'o97scp33', 'send_award', '08208986627', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"379793050041\",\"userId\":\"o97scp33\"},\"id\":\"08208986627\",\"timestamp\":\"2025-09-11 20:41:56.597\"}', 'completed', '2025-09-11 20:41:56', '2025-09-11 20:41:56');
INSERT INTO `task` VALUES (494, 'k5mit6.fk780', 'send_award', '61013386903', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"615737078424\",\"userId\":\"k5mit6.fk780\"},\"id\":\"61013386903\",\"timestamp\":\"2025-09-11 20:41:57.391\"}', 'completed', '2025-09-11 20:41:57', '2025-09-11 20:41:57');
INSERT INTO `task` VALUES (495, 'k5q_io462', 'send_award', '06761984407', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"485424893384\",\"userId\":\"k5q_io462\"},\"id\":\"06761984407\",\"timestamp\":\"2025-09-11 20:41:58.217\"}', 'completed', '2025-09-11 20:41:58', '2025-09-11 20:41:58');
INSERT INTO `task` VALUES (496, 'gztmft22', 'send_award', '02550061334', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"769143912994\",\"userId\":\"gztmft22\"},\"id\":\"02550061334\",\"timestamp\":\"2025-09-11 20:41:59.015\"}', 'completed', '2025-09-11 20:41:59', '2025-09-11 20:41:59');
INSERT INTO `task` VALUES (497, 'ixjit6_tti32', 'send_award', '40573399642', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"161907635283\",\"userId\":\"ixjit6_tti32\"},\"id\":\"40573399642\",\"timestamp\":\"2025-09-11 20:41:59.834\"}', 'completed', '2025-09-11 20:41:59', '2025-09-11 20:41:59');
INSERT INTO `task` VALUES (498, 'fevfoz.kfz23', 'send_award', '09852366118', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"665781895326\",\"userId\":\"fevfoz.kfz23\"},\"id\":\"09852366118\",\"timestamp\":\"2025-09-11 20:42:00.619\"}', 'completed', '2025-09-11 20:42:00', '2025-09-11 20:42:00');
INSERT INTO `task` VALUES (499, 'oq3kgn_hge', 'send_award', '94220109029', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"385794861024\",\"userId\":\"oq3kgn_hge\"},\"id\":\"94220109029\",\"timestamp\":\"2025-09-11 20:42:01.412\"}', 'completed', '2025-09-11 20:42:01', '2025-09-11 20:42:01');
INSERT INTO `task` VALUES (500, 'k7fv9r_fgw', 'send_award', '34019904500', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"192877495933\",\"userId\":\"k7fv9r_fgw\"},\"id\":\"34019904500\",\"timestamp\":\"2025-09-11 20:42:02.312\"}', 'completed', '2025-09-11 20:42:02', '2025-09-11 20:42:02');
INSERT INTO `task` VALUES (501, 'gs2m5656', 'send_award', '20317694359', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"908488826853\",\"userId\":\"gs2m5656\"},\"id\":\"20317694359\",\"timestamp\":\"2025-09-11 20:42:03.134\"}', 'completed', '2025-09-11 20:42:03', '2025-09-11 20:42:03');
INSERT INTO `task` VALUES (502, 'nlrlqi.p1k', 'send_award', '97689016687', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"757896962943\",\"userId\":\"nlrlqi.p1k\"},\"id\":\"97689016687\",\"timestamp\":\"2025-09-11 20:42:03.962\"}', 'completed', '2025-09-11 20:42:04', '2025-09-11 20:42:04');
INSERT INTO `task` VALUES (503, 'ucop8d_i4y87', 'send_award', '81252352006', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"204482846618\",\"userId\":\"ucop8d_i4y87\"},\"id\":\"81252352006\",\"timestamp\":\"2025-09-11 20:42:04.843\"}', 'completed', '2025-09-11 20:42:04', '2025-09-11 20:42:05');
INSERT INTO `task` VALUES (504, 'sf0ppm57', 'send_award', '79434626162', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"190010576210\",\"userId\":\"sf0ppm57\"},\"id\":\"79434626162\",\"timestamp\":\"2025-09-11 20:42:05.642\"}', 'completed', '2025-09-11 20:42:05', '2025-09-11 20:42:05');
INSERT INTO `task` VALUES (505, 'ttbtw9_lr4', 'send_award', '97209884401', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"102266291534\",\"userId\":\"ttbtw9_lr4\"},\"id\":\"97209884401\",\"timestamp\":\"2025-09-11 20:42:06.48\"}', 'completed', '2025-09-11 20:42:06', '2025-09-11 20:42:06');
INSERT INTO `task` VALUES (506, 'k88to334', 'send_award', '72368265838', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"447012892329\",\"userId\":\"k88to334\"},\"id\":\"72368265838\",\"timestamp\":\"2025-09-11 20:42:07.337\"}', 'completed', '2025-09-11 20:42:07', '2025-09-11 20:42:07');
INSERT INTO `task` VALUES (507, 'ttbtw9.i3e86', 'send_award', '17845551789', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"488067867408\",\"userId\":\"ttbtw9.i3e86\"},\"id\":\"17845551789\",\"timestamp\":\"2025-09-11 20:42:08.158\"}', 'completed', '2025-09-11 20:42:08', '2025-09-11 20:42:08');
INSERT INTO `task` VALUES (508, 'izhlq4.oti', 'send_award', '89587777398', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"677122240868\",\"userId\":\"izhlq4.oti\"},\"id\":\"89587777398\",\"timestamp\":\"2025-09-11 20:42:08.95\"}', 'completed', '2025-09-11 20:42:09', '2025-09-11 20:42:09');
INSERT INTO `task` VALUES (509, 'v9r61', 'send_award', '47633548565', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"065044793663\",\"userId\":\"v9r61\"},\"id\":\"47633548565\",\"timestamp\":\"2025-09-11 20:42:09.781\"}', 'completed', '2025-09-11 20:42:09', '2025-09-11 20:42:09');
INSERT INTO `task` VALUES (510, 'lm1sds_rkx', 'send_award', '37633807408', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"540735345140\",\"userId\":\"lm1sds_rkx\"},\"id\":\"37633807408\",\"timestamp\":\"2025-09-11 20:42:10.548\"}', 'completed', '2025-09-11 20:42:10', '2025-09-11 20:42:10');
INSERT INTO `task` VALUES (511, 'ffxlmf_fk177', 'send_award', '69042786566', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"702310873139\",\"userId\":\"ffxlmf_fk177\"},\"id\":\"69042786566\",\"timestamp\":\"2025-09-11 20:42:11.35\"}', 'completed', '2025-09-11 20:42:11', '2025-09-11 20:42:11');
INSERT INTO `task` VALUES (512, 'knnk88.n5fi8w64', 'send_award', '82849459681', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"643114980878\",\"userId\":\"knnk88.n5fi8w64\"},\"id\":\"82849459681\",\"timestamp\":\"2025-09-11 20:42:12.129\"}', 'completed', '2025-09-11 20:42:12', '2025-09-11 20:42:12');
INSERT INTO `task` VALUES (513, 'tt4lj1.fze59', 'send_award', '03302863160', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"319856419701\",\"userId\":\"tt4lj1.fze59\"},\"id\":\"03302863160\",\"timestamp\":\"2025-09-11 20:42:12.893\"}', 'completed', '2025-09-11 20:42:12', '2025-09-11 20:42:13');
INSERT INTO `task` VALUES (514, 'k8qkfk.p9c', 'send_award', '95655634339', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"419561181762\",\"userId\":\"k8qkfk.p9c\"},\"id\":\"95655634339\",\"timestamp\":\"2025-09-11 20:42:13.655\"}', 'completed', '2025-09-11 20:42:13', '2025-09-11 20:42:13');
INSERT INTO `task` VALUES (515, 'ryilj139', 'send_award', '16878134653', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"459035556868\",\"userId\":\"ryilj139\"},\"id\":\"16878134653\",\"timestamp\":\"2025-09-11 20:42:14.475\"}', 'completed', '2025-09-11 20:42:14', '2025-09-11 20:42:14');
INSERT INTO `task` VALUES (516, 'ttbtw9_gxn24', 'send_award', '37034330565', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"716035662307\",\"userId\":\"ttbtw9_gxn24\"},\"id\":\"37034330565\",\"timestamp\":\"2025-09-11 20:42:15.283\"}', 'completed', '2025-09-11 20:42:15', '2025-09-11 20:42:15');
INSERT INTO `task` VALUES (517, 'v9rlnf_iwn40', 'send_award', '37413771593', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"946574004672\",\"userId\":\"v9rlnf_iwn40\"},\"id\":\"37413771593\",\"timestamp\":\"2025-09-11 20:42:16.059\"}', 'completed', '2025-09-11 20:42:16', '2025-09-11 20:42:16');
INSERT INTO `task` VALUES (518, 'k5qkfk_tj383', 'send_award', '11495971477', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"001363804835\",\"userId\":\"k5qkfk_tj383\"},\"id\":\"11495971477\",\"timestamp\":\"2025-09-11 20:42:16.882\"}', 'completed', '2025-09-11 20:42:16', '2025-09-11 20:42:17');
INSERT INTO `task` VALUES (519, 'o97scp_i0k11', 'send_award', '45635212174', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"371400129964\",\"userId\":\"o97scp_i0k11\"},\"id\":\"45635212174\",\"timestamp\":\"2025-09-11 20:42:17.775\"}', 'completed', '2025-09-11 20:42:17', '2025-09-11 20:42:17');
INSERT INTO `task` VALUES (520, 'k7znlr.s3r', 'send_award', '95772806680', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"635597877347\",\"userId\":\"k7znlr.s3r\"},\"id\":\"95772806680\",\"timestamp\":\"2025-09-11 20:42:18.584\"}', 'completed', '2025-09-11 20:42:18', '2025-09-11 20:42:18');
INSERT INTO `task` VALUES (521, 'gs2m5680', 'send_award', '40791011188', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"820604927800\",\"userId\":\"gs2m5680\"},\"id\":\"40791011188\",\"timestamp\":\"2025-09-11 20:42:19.407\"}', 'completed', '2025-09-11 20:42:19', '2025-09-11 20:42:19');
INSERT INTO `task` VALUES (522, 'fmni4832', 'send_award', '97729206068', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"224937399918\",\"userId\":\"fmni4832\"},\"id\":\"97729206068\",\"timestamp\":\"2025-09-11 20:42:20.305\"}', 'completed', '2025-09-11 20:42:20', '2025-09-11 20:42:20');
INSERT INTO `task` VALUES (523, 'o97scp87', 'send_award', '51311968790', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"224062467529\",\"userId\":\"o97scp87\"},\"id\":\"51311968790\",\"timestamp\":\"2025-09-11 20:42:21.005\"}', 'completed', '2025-09-11 20:42:21', '2025-09-11 20:42:21');
INSERT INTO `task` VALUES (524, 'hm1i2v_sp2', 'send_award', '64356743586', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"166150736638\",\"userId\":\"hm1i2v_sp2\"},\"id\":\"64356743586\",\"timestamp\":\"2025-09-11 20:42:21.621\"}', 'completed', '2025-09-11 20:42:21', '2025-09-11 20:42:21');
INSERT INTO `task` VALUES (525, 'k5mmcx.jew', 'send_award', '97600031426', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"844827963310\",\"userId\":\"k5mmcx.jew\"},\"id\":\"97600031426\",\"timestamp\":\"2025-09-11 20:42:22.291\"}', 'completed', '2025-09-11 20:42:22', '2025-09-11 20:42:22');
INSERT INTO `task` VALUES (526, 'tdcsbt.sbq1', 'send_award', '94216563277', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"862019663400\",\"userId\":\"tdcsbt.sbq1\"},\"id\":\"94216563277\",\"timestamp\":\"2025-09-11 20:42:22.93\"}', 'completed', '2025-09-11 20:42:23', '2025-09-11 20:42:23');
INSERT INTO `task` VALUES (527, 'fh5q31_irk', 'send_award', '25031737477', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"763116763002\",\"userId\":\"fh5q31_irk\"},\"id\":\"25031737477\",\"timestamp\":\"2025-09-11 20:42:23.653\"}', 'completed', '2025-09-11 20:42:23', '2025-09-11 20:42:23');
INSERT INTO `task` VALUES (528, 'lh7lnf69', 'send_award', '94497749569', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"526985160531\",\"userId\":\"lh7lnf69\"},\"id\":\"94497749569\",\"timestamp\":\"2025-09-11 20:42:24.45\"}', 'completed', '2025-09-11 20:42:24', '2025-09-11 20:42:24');
INSERT INTO `task` VALUES (529, 'fvpki7.ghc52', 'send_award', '86314492230', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"521007419974\",\"userId\":\"fvpki7.ghc52\"},\"id\":\"86314492230\",\"timestamp\":\"2025-09-11 20:42:25.336\"}', 'completed', '2025-09-11 20:42:25', '2025-09-11 20:42:25');
INSERT INTO `task` VALUES (530, 'tbfnuy40', 'send_award', '64034831006', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"844816569711\",\"userId\":\"tbfnuy40\"},\"id\":\"64034831006\",\"timestamp\":\"2025-09-11 20:42:26.168\"}', 'completed', '2025-09-11 20:42:26', '2025-09-11 20:42:26');
INSERT INTO `task` VALUES (531, 'k5mmcx.n5fi8w', 'send_award', '12315806675', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"737976416703\",\"userId\":\"k5mmcx.n5fi8w\"},\"id\":\"12315806675\",\"timestamp\":\"2025-09-11 20:42:26.983\"}', 'completed', '2025-09-11 20:42:27', '2025-09-11 20:42:27');
INSERT INTO `task` VALUES (532, 'gs2m56_tqo', 'send_award', '99569580406', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"681436236302\",\"userId\":\"gs2m56_tqo\"},\"id\":\"99569580406\",\"timestamp\":\"2025-09-11 20:42:27.821\"}', 'completed', '2025-09-11 20:42:27', '2025-09-11 20:42:27');
INSERT INTO `task` VALUES (533, 'gs2k34_i3h8', 'send_award', '16175672268', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"338432256895\",\"userId\":\"gs2k34_i3h8\"},\"id\":\"16175672268\",\"timestamp\":\"2025-09-11 20:42:28.643\"}', 'completed', '2025-09-11 20:42:28', '2025-09-11 20:42:28');
INSERT INTO `task` VALUES (534, 'jcvsbt_rx7', 'send_award', '15976684752', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"611789078193\",\"userId\":\"jcvsbt_rx7\"},\"id\":\"15976684752\",\"timestamp\":\"2025-09-11 20:42:29.454\"}', 'completed', '2025-09-11 20:42:29', '2025-09-11 20:42:29');
INSERT INTO `task` VALUES (535, 'me4hce.nyg4', 'send_award', '23344457494', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"164505502318\",\"userId\":\"me4hce.nyg4\"},\"id\":\"23344457494\",\"timestamp\":\"2025-09-11 20:42:30.317\"}', 'completed', '2025-09-11 20:42:30', '2025-09-11 20:42:30');
INSERT INTO `task` VALUES (536, 'mzitue.o2g87', 'send_award', '87784136014', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"393834428885\",\"userId\":\"mzitue.o2g87\"},\"id\":\"87784136014\",\"timestamp\":\"2025-09-11 20:42:31.01\"}', 'completed', '2025-09-11 20:42:31', '2025-09-11 20:42:31');
INSERT INTO `task` VALUES (537, 'k88to3.tqj', 'send_award', '86797867605', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"115092865111\",\"userId\":\"k88to3.tqj\"},\"id\":\"86797867605\",\"timestamp\":\"2025-09-11 20:42:31.84\"}', 'completed', '2025-09-11 20:42:31', '2025-09-11 20:42:31');
INSERT INTO `task` VALUES (538, 'ui839', 'send_award', '60369266012', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"198687250750\",\"userId\":\"ui839\"},\"id\":\"60369266012\",\"timestamp\":\"2025-09-11 20:42:32.624\"}', 'completed', '2025-09-11 20:42:32', '2025-09-11 20:42:32');
INSERT INTO `task` VALUES (539, 'lnf_ggu', 'send_award', '84450235484', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"977866713299\",\"userId\":\"lnf_ggu\"},\"id\":\"84450235484\",\"timestamp\":\"2025-09-11 20:42:33.459\"}', 'completed', '2025-09-11 20:42:33', '2025-09-11 20:42:33');
INSERT INTO `task` VALUES (540, 'k8qkfk_ffg84', 'send_award', '33956462050', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"475083682487\",\"userId\":\"k8qkfk_ffg84\"},\"id\":\"33956462050\",\"timestamp\":\"2025-09-11 20:42:34.339\"}', 'completed', '2025-09-11 20:42:34', '2025-09-11 20:42:34');
INSERT INTO `task` VALUES (541, 'k5qscp59', 'send_award', '25179345785', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"133167573381\",\"userId\":\"k5qscp59\"},\"id\":\"25179345785\",\"timestamp\":\"2025-09-11 20:42:35.162\"}', 'completed', '2025-09-11 20:42:35', '2025-09-11 20:42:35');
INSERT INTO `task` VALUES (542, 'lm1sbt6', 'send_award', '13802863525', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"586238808337\",\"userId\":\"lm1sbt6\"},\"id\":\"13802863525\",\"timestamp\":\"2025-09-11 20:42:35.934\"}', 'completed', '2025-09-11 20:42:36', '2025-09-11 20:42:36');
INSERT INTO `task` VALUES (543, 'vacsbt97', 'send_award', '94529886841', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"349139001591\",\"userId\":\"vacsbt97\"},\"id\":\"94529886841\",\"timestamp\":\"2025-09-11 20:42:36.778\"}', 'completed', '2025-09-11 20:42:36', '2025-09-11 20:42:36');
INSERT INTO `task` VALUES (544, 'po4sf0_ib777', 'send_award', '65066499952', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"815868729171\",\"userId\":\"po4sf0_ib777\"},\"id\":\"65066499952\",\"timestamp\":\"2025-09-11 20:42:37.539\"}', 'completed', '2025-09-11 20:42:37', '2025-09-11 20:42:37');
INSERT INTO `task` VALUES (545, 'lmfmhh.ire77', 'send_award', '37610819067', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"803199541856\",\"userId\":\"lmfmhh.ire77\"},\"id\":\"37610819067\",\"timestamp\":\"2025-09-11 20:42:38.31\"}', 'completed', '2025-09-11 20:42:38', '2025-09-11 20:42:38');
INSERT INTO `task` VALUES (546, 'ppm.nz1', 'send_award', '47099561298', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"398530093132\",\"userId\":\"ppm.nz1\"},\"id\":\"47099561298\",\"timestamp\":\"2025-09-11 20:42:39.132\"}', 'completed', '2025-09-11 20:42:39', '2025-09-11 20:42:39');
INSERT INTO `task` VALUES (547, 'i1cvdk65', 'send_award', '09300439629', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"837323220757\",\"userId\":\"i1cvdk65\"},\"id\":\"09300439629\",\"timestamp\":\"2025-09-11 20:42:39.875\"}', 'completed', '2025-09-11 20:42:39', '2025-09-11 20:42:40');
INSERT INTO `task` VALUES (548, 'k88to3_kfc', 'send_award', '18708854828', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"428683916432\",\"userId\":\"k88to3_kfc\"},\"id\":\"18708854828\",\"timestamp\":\"2025-09-11 20:42:40.613\"}', 'completed', '2025-09-11 20:42:40', '2025-09-11 20:42:40');
INSERT INTO `task` VALUES (549, 'sf0ppm.k2860', 'send_award', '44661254612', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"729071627449\",\"userId\":\"sf0ppm.k2860\"},\"id\":\"44661254612\",\"timestamp\":\"2025-09-11 20:42:41.355\"}', 'completed', '2025-09-11 20:42:41', '2025-09-11 20:42:41');
INSERT INTO `task` VALUES (550, 'ggqryd44', 'send_award', '21120799139', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"756770617554\",\"userId\":\"ggqryd44\"},\"id\":\"21120799139\",\"timestamp\":\"2025-09-11 20:42:42.122\"}', 'completed', '2025-09-11 20:42:42', '2025-09-11 20:42:42');
INSERT INTO `task` VALUES (551, 'pybsbt30', 'send_award', '03010605606', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"171704560181\",\"userId\":\"pybsbt30\"},\"id\":\"03010605606\",\"timestamp\":\"2025-09-11 20:42:42.891\"}', 'completed', '2025-09-11 20:42:42', '2025-09-11 20:42:43');
INSERT INTO `task` VALUES (552, 'tt6kfy.g4v78', 'send_award', '92875814452', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"453671620406\",\"userId\":\"tt6kfy.g4v78\"},\"id\":\"92875814452\",\"timestamp\":\"2025-09-11 20:42:43.684\"}', 'completed', '2025-09-11 20:42:43', '2025-09-11 20:42:43');
INSERT INTO `task` VALUES (553, 'ucop8d.glk', 'send_award', '63563749013', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"086628206446\",\"userId\":\"ucop8d.glk\"},\"id\":\"63563749013\",\"timestamp\":\"2025-09-11 20:42:44.478\"}', 'completed', '2025-09-11 20:42:44', '2025-09-11 20:42:44');
INSERT INTO `task` VALUES (554, 'ryilj186', 'send_award', '76002568319', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"018611415498\",\"userId\":\"ryilj186\"},\"id\":\"76002568319\",\"timestamp\":\"2025-09-11 20:42:45.339\"}', 'completed', '2025-09-11 20:42:45', '2025-09-11 20:42:45');
INSERT INTO `task` VALUES (555, 'fmnnzu.keb98', 'send_award', '65905742766', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"345776518808\",\"userId\":\"fmnnzu.keb98\"},\"id\":\"65905742766\",\"timestamp\":\"2025-09-11 20:42:46.156\"}', 'completed', '2025-09-11 20:42:46', '2025-09-11 20:42:46');
INSERT INTO `task` VALUES (556, 'o97fmn21', 'send_award', '62053359936', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"816061191703\",\"userId\":\"o97fmn21\"},\"id\":\"62053359936\",\"timestamp\":\"2025-09-11 20:42:47.198\"}', 'completed', '2025-09-11 20:42:47', '2025-09-11 20:42:47');
INSERT INTO `task` VALUES (557, 'k7ngvs_gbf', 'send_award', '15036249631', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"919983311697\",\"userId\":\"k7ngvs_gbf\"},\"id\":\"15036249631\",\"timestamp\":\"2025-09-11 20:42:48.012\"}', 'completed', '2025-09-11 20:42:48', '2025-09-11 20:42:48');
INSERT INTO `task` VALUES (558, 'ggqryd_i6hsf355', 'send_award', '24977421700', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"812926632238\",\"userId\":\"ggqryd_i6hsf355\"},\"id\":\"24977421700\",\"timestamp\":\"2025-09-11 20:42:48.84\"}', 'completed', '2025-09-11 20:42:48', '2025-09-11 20:42:49');
INSERT INTO `task` VALUES (559, 'i1cuji52', 'send_award', '72317704737', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"120726740982\",\"userId\":\"i1cuji52\"},\"id\":\"72317704737\",\"timestamp\":\"2025-09-11 20:42:49.687\"}', 'completed', '2025-09-11 20:42:49', '2025-09-11 20:42:49');
INSERT INTO `task` VALUES (560, 'pybsbt85', 'send_award', '02602080396', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"200778888041\",\"userId\":\"pybsbt85\"},\"id\":\"02602080396\",\"timestamp\":\"2025-09-11 20:42:50.498\"}', 'completed', '2025-09-11 20:42:50', '2025-09-11 20:42:50');
INSERT INTO `task` VALUES (561, 'lmfmhh_sl5', 'send_award', '24983527305', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"033518193799\",\"userId\":\"lmfmhh_sl5\"},\"id\":\"24983527305\",\"timestamp\":\"2025-09-11 20:42:51.298\"}', 'completed', '2025-09-11 20:42:51', '2025-09-11 20:42:51');
INSERT INTO `task` VALUES (562, 'ts5tbf24', 'send_award', '70441989108', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"677289723362\",\"userId\":\"ts5tbf24\"},\"id\":\"70441989108\",\"timestamp\":\"2025-09-11 20:42:52.044\"}', 'completed', '2025-09-11 20:42:52', '2025-09-11 20:42:52');
INSERT INTO `task` VALUES (563, 'mzitue_sn143', 'send_award', '33581725312', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"291153056326\",\"userId\":\"mzitue_sn143\"},\"id\":\"33581725312\",\"timestamp\":\"2025-09-11 20:42:52.891\"}', 'completed', '2025-09-11 20:42:52', '2025-09-11 20:42:53');
INSERT INTO `task` VALUES (564, 'fevfoz.lpi22', 'send_award', '36989514231', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"351834030681\",\"userId\":\"fevfoz.lpi22\"},\"id\":\"36989514231\",\"timestamp\":\"2025-09-11 20:42:53.668\"}', 'completed', '2025-09-11 20:42:53', '2025-09-11 20:42:53');
INSERT INTO `task` VALUES (565, 'gs2m56_ggl72', 'send_award', '53922120857', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"846235838309\",\"userId\":\"gs2m56_ggl72\"},\"id\":\"53922120857\",\"timestamp\":\"2025-09-11 20:42:54.499\"}', 'completed', '2025-09-11 20:42:54', '2025-09-11 20:42:54');
INSERT INTO `task` VALUES (566, 'mzitue.tcu0', 'send_award', '78073019893', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"901646401115\",\"userId\":\"mzitue.tcu0\"},\"id\":\"78073019893\",\"timestamp\":\"2025-09-11 20:42:55.299\"}', 'completed', '2025-09-11 20:42:55', '2025-09-11 20:42:55');
INSERT INTO `task` VALUES (567, 'izhsf0_go824', 'send_award', '69345201205', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"275910238047\",\"userId\":\"izhsf0_go824\"},\"id\":\"69345201205\",\"timestamp\":\"2025-09-11 20:42:56.128\"}', 'completed', '2025-09-11 20:42:56', '2025-09-11 20:42:56');
INSERT INTO `task` VALUES (568, 'negtue_psi98', 'send_award', '73811584746', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"089682630429\",\"userId\":\"negtue_psi98\"},\"id\":\"73811584746\",\"timestamp\":\"2025-09-11 20:42:56.893\"}', 'completed', '2025-09-11 20:42:56', '2025-09-11 20:42:57');
INSERT INTO `task` VALUES (569, 'izhpd6_iwd81', 'send_award', '07583148380', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"052387100699\",\"userId\":\"izhpd6_iwd81\"},\"id\":\"07583148380\",\"timestamp\":\"2025-09-11 20:42:57.671\"}', 'completed', '2025-09-11 20:42:57', '2025-09-11 20:42:57');
INSERT INTO `task` VALUES (570, 'jcvsbt.fh869', 'send_award', '64044030540', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"104174547688\",\"userId\":\"jcvsbt.fh869\"},\"id\":\"64044030540\",\"timestamp\":\"2025-09-11 20:42:58.527\"}', 'completed', '2025-09-11 20:42:58', '2025-09-11 20:42:58');
INSERT INTO `task` VALUES (571, 'lzbmd2.fqn', 'send_award', '69147028124', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"319761523912\",\"userId\":\"lzbmd2.fqn\"},\"id\":\"69147028124\",\"timestamp\":\"2025-09-11 20:42:59.303\"}', 'completed', '2025-09-11 20:42:59', '2025-09-11 20:42:59');
INSERT INTO `task` VALUES (572, 'o97kgs_ghb18', 'send_award', '96605981902', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"403554336722\",\"userId\":\"o97kgs_ghb18\"},\"id\":\"96605981902\",\"timestamp\":\"2025-09-11 20:43:00.055\"}', 'completed', '2025-09-11 20:43:00', '2025-09-11 20:43:00');
INSERT INTO `task` VALUES (573, 'g6nmzi_irk', 'send_award', '33344546968', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"277454856421\",\"userId\":\"g6nmzi_irk\"},\"id\":\"33344546968\",\"timestamp\":\"2025-09-11 20:43:00.858\"}', 'completed', '2025-09-11 20:43:00', '2025-09-11 20:43:01');
INSERT INTO `task` VALUES (574, 'mawnuy_hce', 'send_award', '27954263010', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"493552799216\",\"userId\":\"mawnuy_hce\"},\"id\":\"27954263010\",\"timestamp\":\"2025-09-11 20:43:01.678\"}', 'completed', '2025-09-11 20:43:01', '2025-09-11 20:43:01');
INSERT INTO `task` VALUES (575, 'pd6fvp_ssq', 'send_award', '44956898990', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"639930962915\",\"userId\":\"pd6fvp_ssq\"},\"id\":\"44956898990\",\"timestamp\":\"2025-09-11 20:43:02.481\"}', 'completed', '2025-09-11 20:43:02', '2025-09-11 20:43:02');
INSERT INTO `task` VALUES (576, 'tw6my276', 'send_award', '23540554627', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"071068426153\",\"userId\":\"tw6my276\"},\"id\":\"23540554627\",\"timestamp\":\"2025-09-11 20:43:03.316\"}', 'completed', '2025-09-11 20:43:03', '2025-09-11 20:43:03');
INSERT INTO `task` VALUES (577, 'mzitue_nj340', 'send_award', '26916901797', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"906419520345\",\"userId\":\"mzitue_nj340\"},\"id\":\"26916901797\",\"timestamp\":\"2025-09-11 20:43:04.153\"}', 'completed', '2025-09-11 20:43:04', '2025-09-11 20:43:04');
INSERT INTO `task` VALUES (578, 'pa8kfk6', 'send_award', '16189743742', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"812651958318\",\"userId\":\"pa8kfk6\"},\"id\":\"16189743742\",\"timestamp\":\"2025-09-11 20:43:04.964\"}', 'completed', '2025-09-11 20:43:05', '2025-09-11 20:43:05');
INSERT INTO `task` VALUES (579, 'k8qi48_tml1', 'send_award', '38967112255', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"811233753965\",\"userId\":\"k8qi48_tml1\"},\"id\":\"38967112255\",\"timestamp\":\"2025-09-11 20:43:05.825\"}', 'completed', '2025-09-11 20:43:05', '2025-09-11 20:43:05');
INSERT INTO `task` VALUES (580, 'm8eiuk_g3f', 'send_award', '81974069775', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"501632111531\",\"userId\":\"m8eiuk_g3f\"},\"id\":\"81974069775\",\"timestamp\":\"2025-09-11 20:43:06.606\"}', 'completed', '2025-09-11 20:43:06', '2025-09-11 20:43:06');
INSERT INTO `task` VALUES (581, 'ktzmzi_scp', 'send_award', '47833171071', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"017015492394\",\"userId\":\"ktzmzi_scp\"},\"id\":\"47833171071\",\"timestamp\":\"2025-09-11 20:43:07.408\"}', 'completed', '2025-09-11 20:43:07', '2025-09-11 20:43:07');
INSERT INTO `task` VALUES (582, 'n0emzg_tn27', 'send_award', '51672697978', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"503860844003\",\"userId\":\"n0emzg_tn27\"},\"id\":\"51672697978\",\"timestamp\":\"2025-09-11 20:43:08.187\"}', 'completed', '2025-09-11 20:43:08', '2025-09-11 20:43:08');
INSERT INTO `task` VALUES (583, 'k1zsbt_l9x91', 'send_award', '20023907276', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"075525008239\",\"userId\":\"k1zsbt_l9x91\"},\"id\":\"20023907276\",\"timestamp\":\"2025-09-11 20:43:08.942\"}', 'completed', '2025-09-11 20:43:09', '2025-09-11 20:43:09');
INSERT INTO `task` VALUES (584, 'lmfmhh_kd7', 'send_award', '44506656809', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"868485637198\",\"userId\":\"lmfmhh_kd7\"},\"id\":\"44506656809\",\"timestamp\":\"2025-09-11 20:43:09.779\"}', 'completed', '2025-09-11 20:43:09', '2025-09-11 20:43:09');
INSERT INTO `task` VALUES (585, 'k5qsbt.oar93', 'send_award', '47921086417', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"034321735675\",\"userId\":\"k5qsbt.oar93\"},\"id\":\"47921086417\",\"timestamp\":\"2025-09-11 20:43:10.547\"}', 'completed', '2025-09-11 20:43:10', '2025-09-11 20:43:10');
INSERT INTO `task` VALUES (586, 'k18iui_tn253', 'send_award', '01346083663', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"184983286687\",\"userId\":\"k18iui_tn253\"},\"id\":\"01346083663\",\"timestamp\":\"2025-09-11 20:43:11.322\"}', 'completed', '2025-09-11 20:43:11', '2025-09-11 20:43:11');
INSERT INTO `task` VALUES (587, 'k18iui_ist', 'send_award', '26745056268', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"462978645860\",\"userId\":\"k18iui_ist\"},\"id\":\"26745056268\",\"timestamp\":\"2025-09-11 20:43:12.082\"}', 'completed', '2025-09-11 20:43:12', '2025-09-11 20:43:12');
INSERT INTO `task` VALUES (588, 'rydkjv_qj262', 'send_award', '05207046255', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"231057891225\",\"userId\":\"rydkjv_qj262\"},\"id\":\"05207046255\",\"timestamp\":\"2025-09-11 20:43:12.933\"}', 'completed', '2025-09-11 20:43:13', '2025-09-11 20:43:13');
INSERT INTO `task` VALUES (589, 'i1cl5v.ioq82', 'send_award', '12301006035', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"523223098524\",\"userId\":\"i1cl5v.ioq82\"},\"id\":\"12301006035\",\"timestamp\":\"2025-09-11 20:43:13.775\"}', 'completed', '2025-09-11 20:43:13', '2025-09-11 20:43:13');
INSERT INTO `task` VALUES (590, 'v9rmdm75', 'send_award', '37834259544', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"822067077994\",\"userId\":\"v9rmdm75\"},\"id\":\"37834259544\",\"timestamp\":\"2025-09-11 20:43:14.568\"}', 'completed', '2025-09-11 20:43:14', '2025-09-11 20:43:14');
INSERT INTO `task` VALUES (591, 'hm1nuy_tt6', 'send_award', '73491874303', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"542128510344\",\"userId\":\"hm1nuy_tt6\"},\"id\":\"73491874303\",\"timestamp\":\"2025-09-11 20:43:15.389\"}', 'completed', '2025-09-11 20:43:15', '2025-09-11 20:43:15');
INSERT INTO `task` VALUES (592, 'ggqk1z.fj980', 'send_award', '29878243659', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"581537762946\",\"userId\":\"ggqk1z.fj980\"},\"id\":\"29878243659\",\"timestamp\":\"2025-09-11 20:43:16.388\"}', 'completed', '2025-09-11 20:43:16', '2025-09-11 20:43:16');
INSERT INTO `task` VALUES (593, 'ryilj19', 'send_award', '97388223105', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"761018521456\",\"userId\":\"ryilj19\"},\"id\":\"97388223105\",\"timestamp\":\"2025-09-11 20:43:17.08\"}', 'completed', '2025-09-11 20:43:17', '2025-09-11 20:43:17');
INSERT INTO `task` VALUES (594, 'ppm_sks30', 'send_award', '80805530763', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"603113690564\",\"userId\":\"ppm_sks30\"},\"id\":\"80805530763\",\"timestamp\":\"2025-09-11 20:43:17.719\"}', 'completed', '2025-09-11 20:43:17', '2025-09-11 20:43:17');
INSERT INTO `task` VALUES (595, 'hm1i2v26', 'send_award', '96724005542', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"746457778173\",\"userId\":\"hm1i2v26\"},\"id\":\"96724005542\",\"timestamp\":\"2025-09-11 20:43:18.63\"}', 'completed', '2025-09-11 20:43:18', '2025-09-11 20:43:18');
INSERT INTO `task` VALUES (596, 'ffxlmf_lrs54', 'send_award', '70380881502', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"960946217886\",\"userId\":\"ffxlmf_lrs54\"},\"id\":\"70380881502\",\"timestamp\":\"2025-09-11 20:43:19.435\"}', 'completed', '2025-09-11 20:43:19', '2025-09-11 20:43:19');
INSERT INTO `task` VALUES (597, 'ggqk1z31', 'send_award', '03076990815', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"273433046821\",\"userId\":\"ggqk1z31\"},\"id\":\"03076990815\",\"timestamp\":\"2025-09-11 20:43:20.191\"}', 'completed', '2025-09-11 20:43:20', '2025-09-11 20:43:20');
INSERT INTO `task` VALUES (598, 'ts5hvb_ssq', 'send_award', '52744291464', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"852348661153\",\"userId\":\"ts5hvb_ssq\"},\"id\":\"52744291464\",\"timestamp\":\"2025-09-11 20:43:21.19\"}', 'completed', '2025-09-11 20:43:21', '2025-09-11 20:43:21');
INSERT INTO `task` VALUES (599, 'lh7lnf.g53', 'send_award', '15270715007', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"218012945362\",\"userId\":\"lh7lnf.g53\"},\"id\":\"15270715007\",\"timestamp\":\"2025-09-11 20:43:21.985\"}', 'completed', '2025-09-11 20:43:22', '2025-09-11 20:43:22');
INSERT INTO `task` VALUES (600, 'lh7k1z_i46', 'send_award', '72517474071', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"803498049674\",\"userId\":\"lh7k1z_i46\"},\"id\":\"72517474071\",\"timestamp\":\"2025-09-11 20:43:22.901\"}', 'completed', '2025-09-11 20:43:22', '2025-09-11 20:43:23');
INSERT INTO `task` VALUES (601, 'mawfmn57', 'send_award', '48974614922', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"453407879495\",\"userId\":\"mawfmn57\"},\"id\":\"48974614922\",\"timestamp\":\"2025-09-11 20:43:23.722\"}', 'completed', '2025-09-11 20:43:23', '2025-09-11 20:43:23');
INSERT INTO `task` VALUES (602, 'pybsbt.fnq', 'send_award', '97388525627', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"595087657700\",\"userId\":\"pybsbt.fnq\"},\"id\":\"97388525627\",\"timestamp\":\"2025-09-11 20:43:24.591\"}', 'completed', '2025-09-11 20:43:24', '2025-09-11 20:43:24');
INSERT INTO `task` VALUES (603, 'i8lv9r.jdb', 'send_award', '39143698653', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"792496894843\",\"userId\":\"i8lv9r.jdb\"},\"id\":\"39143698653\",\"timestamp\":\"2025-09-11 20:43:25.388\"}', 'completed', '2025-09-11 20:43:25', '2025-09-11 20:43:25');
INSERT INTO `task` VALUES (604, 'gs2k34_nez', 'send_award', '46924621278', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"431030825356\",\"userId\":\"gs2k34_nez\"},\"id\":\"46924621278\",\"timestamp\":\"2025-09-11 20:43:26.188\"}', 'completed', '2025-09-11 20:43:26', '2025-09-11 20:43:26');
INSERT INTO `task` VALUES (605, 'ts5hvb.q7z14', 'send_award', '22484022735', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"846782563149\",\"userId\":\"ts5hvb.q7z14\"},\"id\":\"22484022735\",\"timestamp\":\"2025-09-11 20:43:27.053\"}', 'completed', '2025-09-11 20:43:27', '2025-09-11 20:43:27');
INSERT INTO `task` VALUES (606, 'tw9izl_puf', 'send_award', '20804736987', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"000636486649\",\"userId\":\"tw9izl_puf\"},\"id\":\"20804736987\",\"timestamp\":\"2025-09-11 20:43:27.793\"}', 'completed', '2025-09-11 20:43:27', '2025-09-11 20:43:27');
INSERT INTO `task` VALUES (607, 'i1cvdk.sk766', 'send_award', '54425400947', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"567337063279\",\"userId\":\"i1cvdk.sk766\"},\"id\":\"54425400947\",\"timestamp\":\"2025-09-11 20:43:28.555\"}', 'completed', '2025-09-11 20:43:28', '2025-09-11 20:43:28');
INSERT INTO `task` VALUES (608, 'is8k1z_fgs', 'send_award', '45227551200', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"804363396279\",\"userId\":\"is8k1z_fgs\"},\"id\":\"45227551200\",\"timestamp\":\"2025-09-11 20:43:29.3\"}', 'completed', '2025-09-11 20:43:29', '2025-09-11 20:43:29');
INSERT INTO `task` VALUES (609, 'jcvsbt87', 'send_award', '40591869947', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"294551180693\",\"userId\":\"jcvsbt87\"},\"id\":\"40591869947\",\"timestamp\":\"2025-09-11 20:43:30.056\"}', 'completed', '2025-09-11 20:43:30', '2025-09-11 20:43:30');
INSERT INTO `task` VALUES (610, 'gs2k34_u4f88', 'send_award', '73771484748', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"126225669348\",\"userId\":\"gs2k34_u4f88\"},\"id\":\"73771484748\",\"timestamp\":\"2025-09-11 20:43:30.892\"}', 'completed', '2025-09-11 20:43:30', '2025-09-11 20:43:31');
INSERT INTO `task` VALUES (611, 'k5mmcx74', 'send_award', '87964660724', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"735137035944\",\"userId\":\"k5mmcx74\"},\"id\":\"87964660724\",\"timestamp\":\"2025-09-11 20:43:31.704\"}', 'completed', '2025-09-11 20:43:31', '2025-09-11 20:43:31');
INSERT INTO `task` VALUES (612, 'k5mmcx.qg973', 'send_award', '81002210115', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"821081860164\",\"userId\":\"k5mmcx.qg973\"},\"id\":\"81002210115\",\"timestamp\":\"2025-09-11 20:43:32.526\"}', 'completed', '2025-09-11 20:43:32', '2025-09-11 20:43:32');
INSERT INTO `task` VALUES (613, 'oq3kgn.gl4o6y10', 'send_award', '90210941208', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"315521815970\",\"userId\":\"oq3kgn.gl4o6y10\"},\"id\":\"90210941208\",\"timestamp\":\"2025-09-11 20:43:33.316\"}', 'completed', '2025-09-11 20:43:33', '2025-09-11 20:43:33');
INSERT INTO `task` VALUES (614, 'k7ngvs.gky42', 'send_award', '11867047584', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"250530534925\",\"userId\":\"k7ngvs.gky42\"},\"id\":\"11867047584\",\"timestamp\":\"2025-09-11 20:43:34.101\"}', 'completed', '2025-09-11 20:43:34', '2025-09-11 20:43:34');
INSERT INTO `task` VALUES (615, 'ffxlmf_gqg33', 'send_award', '05057240432', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"083921769796\",\"userId\":\"ffxlmf_gqg33\"},\"id\":\"05057240432\",\"timestamp\":\"2025-09-11 20:43:34.926\"}', 'completed', '2025-09-11 20:43:35', '2025-09-11 20:43:35');
INSERT INTO `task` VALUES (616, 'fvpts4_qg435', 'send_award', '15764612144', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"174887400116\",\"userId\":\"fvpts4_qg435\"},\"id\":\"15764612144\",\"timestamp\":\"2025-09-11 20:43:35.731\"}', 'completed', '2025-09-11 20:43:35', '2025-09-11 20:43:35');
INSERT INTO `task` VALUES (617, 'lm1i2v63', 'send_award', '19962592492', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"604611442316\",\"userId\":\"lm1i2v63\"},\"id\":\"19962592492\",\"timestamp\":\"2025-09-11 20:43:36.465\"}', 'completed', '2025-09-11 20:43:36', '2025-09-11 20:43:36');
INSERT INTO `task` VALUES (618, 'i1clo5_oti', 'send_award', '43771699151', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"336707501296\",\"userId\":\"i1clo5_oti\"},\"id\":\"43771699151\",\"timestamp\":\"2025-09-11 20:43:37.276\"}', 'completed', '2025-09-11 20:43:37', '2025-09-11 20:43:37');
INSERT INTO `task` VALUES (619, 'oq3kgn39', 'send_award', '78245766264', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"549524966738\",\"userId\":\"oq3kgn39\"},\"id\":\"78245766264\",\"timestamp\":\"2025-09-11 20:43:38.017\"}', 'completed', '2025-09-11 20:43:38', '2025-09-11 20:43:38');
INSERT INTO `task` VALUES (620, 'fmnnzu.sib', 'send_award', '47969718374', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"598280794989\",\"userId\":\"fmnnzu.sib\"},\"id\":\"47969718374\",\"timestamp\":\"2025-09-11 20:43:38.823\"}', 'completed', '2025-09-11 20:43:38', '2025-09-11 20:43:39');
INSERT INTO `task` VALUES (621, 'k18iui23', 'send_award', '24402785715', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"626574273186\",\"userId\":\"k18iui23\"},\"id\":\"24402785715\",\"timestamp\":\"2025-09-11 20:43:39.667\"}', 'completed', '2025-09-11 20:43:39', '2025-09-11 20:43:39');
INSERT INTO `task` VALUES (622, 'hm1nuy_lid', 'send_award', '32875467433', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"394242850896\",\"userId\":\"hm1nuy_lid\"},\"id\":\"32875467433\",\"timestamp\":\"2025-09-11 20:43:40.413\"}', 'completed', '2025-09-11 20:43:40', '2025-09-11 20:43:40');
INSERT INTO `task` VALUES (623, 'jlbffg18', 'send_award', '76521349551', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"030092172986\",\"userId\":\"jlbffg18\"},\"id\":\"76521349551\",\"timestamp\":\"2025-09-11 20:43:41.216\"}', 'completed', '2025-09-11 20:43:41', '2025-09-11 20:43:41');
INSERT INTO `task` VALUES (624, 'pgknz911', 'send_award', '44654995905', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"368273962486\",\"userId\":\"pgknz911\"},\"id\":\"44654995905\",\"timestamp\":\"2025-09-11 20:43:42.045\"}', 'completed', '2025-09-11 20:43:42', '2025-09-11 20:43:42');
INSERT INTO `task` VALUES (625, 'k1zu0c_skj34', 'send_award', '29782730997', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"040699385942\",\"userId\":\"k1zu0c_skj34\"},\"id\":\"29782730997\",\"timestamp\":\"2025-09-11 20:43:42.822\"}', 'completed', '2025-09-11 20:43:42', '2025-09-11 20:43:42');
INSERT INTO `task` VALUES (626, 'jlbffg20', 'send_award', '59469355875', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"323886762199\",\"userId\":\"jlbffg20\"},\"id\":\"59469355875\",\"timestamp\":\"2025-09-11 20:43:43.65\"}', 'completed', '2025-09-11 20:43:43', '2025-09-11 20:43:43');
INSERT INTO `task` VALUES (627, 'vacsbt86', 'send_award', '12082556698', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"442279039053\",\"userId\":\"vacsbt86\"},\"id\":\"12082556698\",\"timestamp\":\"2025-09-11 20:43:44.406\"}', 'completed', '2025-09-11 20:43:44', '2025-09-11 20:43:44');
INSERT INTO `task` VALUES (628, 'v9bmdm.s3r72', 'send_award', '29866736547', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"152214386602\",\"userId\":\"v9bmdm.s3r72\"},\"id\":\"29866736547\",\"timestamp\":\"2025-09-11 20:43:45.202\"}', 'completed', '2025-09-11 20:43:45', '2025-09-11 20:43:45');
INSERT INTO `task` VALUES (629, 'jzzgm3_ntz', 'send_award', '15204956492', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"597975380190\",\"userId\":\"jzzgm3_ntz\"},\"id\":\"15204956492\",\"timestamp\":\"2025-09-11 20:43:45.986\"}', 'completed', '2025-09-11 20:43:46', '2025-09-11 20:43:46');
INSERT INTO `task` VALUES (630, 'k8qlqi.nel95', 'send_award', '43456243804', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"126181316058\",\"userId\":\"k8qlqi.nel95\"},\"id\":\"43456243804\",\"timestamp\":\"2025-09-11 20:43:46.788\"}', 'completed', '2025-09-11 20:43:46', '2025-09-11 20:43:46');
INSERT INTO `task` VALUES (631, 'gztmft.i85', 'send_award', '55357955022', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"476854803183\",\"userId\":\"gztmft.i85\"},\"id\":\"55357955022\",\"timestamp\":\"2025-09-11 20:43:47.588\"}', 'completed', '2025-09-11 20:43:47', '2025-09-11 20:43:47');
INSERT INTO `task` VALUES (632, 'izhlu8_kgs62', 'send_award', '34469930168', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"699283069554\",\"userId\":\"izhlu8_kgs62\"},\"id\":\"34469930168\",\"timestamp\":\"2025-09-11 20:43:48.39\"}', 'completed', '2025-09-11 20:43:48', '2025-09-11 20:43:48');
INSERT INTO `task` VALUES (633, 'sf0ppm_onw', 'send_award', '86024362388', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"353513340001\",\"userId\":\"sf0ppm_onw\"},\"id\":\"86024362388\",\"timestamp\":\"2025-09-11 20:43:49.155\"}', 'completed', '2025-09-11 20:43:49', '2025-09-11 20:43:49');
INSERT INTO `task` VALUES (634, 'u6mgge_sfi', 'send_award', '33946051375', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"522475732741\",\"userId\":\"u6mgge_sfi\"},\"id\":\"33946051375\",\"timestamp\":\"2025-09-11 20:43:49.985\"}', 'completed', '2025-09-11 20:43:50', '2025-09-11 20:43:50');
INSERT INTO `task` VALUES (635, 'hm1hs7_fep', 'send_award', '46970028436', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"094317938876\",\"userId\":\"hm1hs7_fep\"},\"id\":\"46970028436\",\"timestamp\":\"2025-09-11 20:43:50.838\"}', 'completed', '2025-09-11 20:43:50', '2025-09-11 20:43:51');
INSERT INTO `task` VALUES (636, 'mzitue_lzw', 'send_award', '32817610046', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"513797830139\",\"userId\":\"mzitue_lzw\"},\"id\":\"32817610046\",\"timestamp\":\"2025-09-11 20:43:51.654\"}', 'completed', '2025-09-11 20:43:51', '2025-09-11 20:43:51');
INSERT INTO `task` VALUES (637, 'fgwuih.gkv94', 'send_award', '82785633571', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"296798147319\",\"userId\":\"fgwuih.gkv94\"},\"id\":\"82785633571\",\"timestamp\":\"2025-09-11 20:43:52.482\"}', 'completed', '2025-09-11 20:43:52', '2025-09-11 20:43:52');
INSERT INTO `task` VALUES (638, 'ttbtw9_hlm32', 'send_award', '66745455201', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"561448789939\",\"userId\":\"ttbtw9_hlm32\"},\"id\":\"66745455201\",\"timestamp\":\"2025-09-11 20:43:53.27\"}', 'completed', '2025-09-11 20:43:53', '2025-09-11 20:43:53');
INSERT INTO `task` VALUES (639, 'me4nzu15', 'send_award', '70003293875', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"618605856813\",\"userId\":\"me4nzu15\"},\"id\":\"70003293875\",\"timestamp\":\"2025-09-11 20:43:54.058\"}', 'completed', '2025-09-11 20:43:54', '2025-09-11 20:43:54');
INSERT INTO `task` VALUES (640, 'lm1_tti', 'send_award', '21368945973', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"119920536560\",\"userId\":\"lm1_tti\"},\"id\":\"21368945973\",\"timestamp\":\"2025-09-11 20:43:54.83\"}', 'completed', '2025-09-11 20:43:54', '2025-09-11 20:43:55');
INSERT INTO `task` VALUES (641, 'jay', 'send_award', '11611397502', '{\"data\":{\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"425692335025\",\"userId\":\"jay\"},\"id\":\"11611397502\",\"timestamp\":\"2025-09-12 12:14:34.224\"}', 'completed', '2025-09-12 12:14:34', '2025-09-12 12:14:34');

-- ----------------------------
-- Table structure for user_award_record_000
-- ----------------------------
DROP TABLE IF EXISTS `user_award_record_000`;
CREATE TABLE `user_award_record_000`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `strategy_id` bigint NOT NULL COMMENT '抽奖策略ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '抽奖订单ID【作为幂等使用】',
  `award_id` int NOT NULL COMMENT '奖品ID',
  `award_title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '奖品标题（名称）',
  `award_time` datetime NOT NULL COMMENT '中奖时间',
  `award_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'create' COMMENT '奖品状态；create-创建、completed-发奖完成',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_activity_id`(`activity_id` ASC) USING BTREE,
  INDEX `idx_award_id`(`strategy_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 60 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户中奖记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_award_record_000
-- ----------------------------
INSERT INTO `user_award_record_000` VALUES (1, 'p72k3d55', 100301, 100006, '249672253957', 102, 'OpenAI会员卡', '2025-09-11 12:40:38', 'create', '2025-09-11 20:40:38', '2025-09-11 20:40:38');
INSERT INTO `user_award_record_000` VALUES (2, 'v9blnf12', 100301, 100006, '926235935225', 101, '随机积分', '2025-09-11 12:40:42', 'completed', '2025-09-11 20:40:41', '2025-09-11 20:40:41');
INSERT INTO `user_award_record_000` VALUES (3, 'k8qkfk86', 100301, 100006, '716151311315', 101, '随机积分', '2025-09-11 12:40:44', 'completed', '2025-09-11 20:40:43', '2025-09-11 20:40:43');
INSERT INTO `user_award_record_000` VALUES (4, 'irescp24', 100301, 100006, '262982027709', 102, 'OpenAI会员卡', '2025-09-11 12:40:45', 'create', '2025-09-11 20:40:45', '2025-09-11 20:40:45');
INSERT INTO `user_award_record_000` VALUES (5, 'mzitue_ol733', 100301, 100006, '878501532647', 108, '暴走玩偶', '2025-09-11 12:40:52', 'create', '2025-09-11 20:40:52', '2025-09-11 20:40:52');
INSERT INTO `user_award_record_000` VALUES (6, 'ixjit6.laj61', 100301, 100006, '108813262747', 107, '小霸王游戏机', '2025-09-11 12:40:54', 'create', '2025-09-11 20:40:54', '2025-09-11 20:40:54');
INSERT INTO `user_award_record_000` VALUES (7, 'oq3kgn38', 100301, 100006, '445209923164', 108, '暴走玩偶', '2025-09-11 12:40:58', 'create', '2025-09-11 20:40:57', '2025-09-11 20:40:57');
INSERT INTO `user_award_record_000` VALUES (8, 'kgs_man44', 100301, 100006, '651754461929', 101, '随机积分', '2025-09-11 12:40:58', 'completed', '2025-09-11 20:40:58', '2025-09-11 20:40:58');
INSERT INTO `user_award_record_000` VALUES (9, 'pa8kfk35', 100301, 100006, '278771929782', 103, '支付优惠券', '2025-09-11 12:41:05', 'create', '2025-09-11 20:41:04', '2025-09-11 20:41:04');
INSERT INTO `user_award_record_000` VALUES (10, 'i1cvdk88', 100301, 100006, '997486405389', 107, '小霸王游戏机', '2025-09-11 12:41:06', 'create', '2025-09-11 20:41:06', '2025-09-11 20:41:06');
INSERT INTO `user_award_record_000` VALUES (11, 'k5qkfk.qap8', 100301, 100006, '903258584118', 101, '随机积分', '2025-09-11 12:41:11', 'completed', '2025-09-11 20:41:10', '2025-09-11 20:41:10');
INSERT INTO `user_award_record_000` VALUES (12, 'k8qi48.pip39', 100301, 100006, '695425801466', 101, '随机积分', '2025-09-11 12:41:17', 'completed', '2025-09-11 20:41:17', '2025-09-11 20:41:17');
INSERT INTO `user_award_record_000` VALUES (13, 'lh7lnf96', 100301, 100006, '826087427643', 102, 'OpenAI会员卡', '2025-09-11 12:41:20', 'create', '2025-09-11 20:41:19', '2025-09-11 20:41:19');
INSERT INTO `user_award_record_000` VALUES (14, 'fh5q31.ljf', 100301, 100006, '860452415786', 104, '小米台灯', '2025-09-11 12:41:21', 'create', '2025-09-11 20:41:20', '2025-09-11 20:41:20');
INSERT INTO `user_award_record_000` VALUES (15, 'gs2k34_ny930', 100301, 100006, '615093648577', 101, '随机积分', '2025-09-11 12:41:21', 'completed', '2025-09-11 20:41:21', '2025-09-11 20:41:21');
INSERT INTO `user_award_record_000` VALUES (16, 'tt4lj1_vix54', 100301, 100006, '516407902610', 108, '暴走玩偶', '2025-09-11 12:41:30', 'create', '2025-09-11 20:41:30', '2025-09-11 20:41:30');
INSERT INTO `user_award_record_000` VALUES (17, 'rydkjv.g3k46', 100301, 100006, '580317468716', 108, '暴走玩偶', '2025-09-11 12:41:33', 'create', '2025-09-11 20:41:33', '2025-09-11 20:41:33');
INSERT INTO `user_award_record_000` VALUES (18, 'p72k3d.rvz', 100301, 100006, '921974904299', 101, '随机积分', '2025-09-11 12:41:38', 'completed', '2025-09-11 20:41:38', '2025-09-11 20:41:38');
INSERT INTO `user_award_record_000` VALUES (19, 'o97scp88', 100301, 100006, '102545732985', 104, '小米台灯', '2025-09-11 12:41:45', 'create', '2025-09-11 20:41:44', '2025-09-11 20:41:44');
INSERT INTO `user_award_record_000` VALUES (20, 'izhlq423', 100301, 100006, '948359516153', 102, 'OpenAI会员卡', '2025-09-11 12:41:45', 'create', '2025-09-11 20:41:45', '2025-09-11 20:41:45');
INSERT INTO `user_award_record_000` VALUES (21, 'p1pvgw84', 100301, 100006, '581273917437', 108, '暴走玩偶', '2025-09-11 12:41:47', 'create', '2025-09-11 20:41:46', '2025-09-11 20:41:46');
INSERT INTO `user_award_record_000` VALUES (22, 'k5mmcx_ny262', 100301, 100006, '893159250084', 102, 'OpenAI会员卡', '2025-09-11 12:41:54', 'create', '2025-09-11 20:41:54', '2025-09-11 20:41:54');
INSERT INTO `user_award_record_000` VALUES (23, 'k18to3.lg016', 100301, 100006, '040552304849', 103, '支付优惠券', '2025-09-11 12:41:55', 'create', '2025-09-11 20:41:55', '2025-09-11 20:41:55');
INSERT INTO `user_award_record_000` VALUES (24, 'o97scp33', 100301, 100006, '379793050041', 102, 'OpenAI会员卡', '2025-09-11 12:41:57', 'create', '2025-09-11 20:41:56', '2025-09-11 20:41:56');
INSERT INTO `user_award_record_000` VALUES (25, 'ixjit6_tti32', 100301, 100006, '161907635283', 107, '小霸王游戏机', '2025-09-11 12:42:00', 'create', '2025-09-11 20:41:59', '2025-09-11 20:41:59');
INSERT INTO `user_award_record_000` VALUES (26, 'k7fv9r_fgw', 100301, 100006, '192877495933', 101, '随机积分', '2025-09-11 12:42:02', 'completed', '2025-09-11 20:42:02', '2025-09-11 20:42:02');
INSERT INTO `user_award_record_000` VALUES (27, 'sf0ppm57', 100301, 100006, '190010576210', 104, '小米台灯', '2025-09-11 12:42:06', 'create', '2025-09-11 20:42:05', '2025-09-11 20:42:05');
INSERT INTO `user_award_record_000` VALUES (28, 'izhlq4.oti', 100301, 100006, '677122240868', 101, '随机积分', '2025-09-11 12:42:09', 'completed', '2025-09-11 20:42:09', '2025-09-11 20:42:09');
INSERT INTO `user_award_record_000` VALUES (29, 'v9r61', 100301, 100006, '065044793663', 103, '支付优惠券', '2025-09-11 12:42:10', 'create', '2025-09-11 20:42:09', '2025-09-11 20:42:09');
INSERT INTO `user_award_record_000` VALUES (30, 'ryilj139', 100301, 100006, '459035556868', 103, '支付优惠券', '2025-09-11 12:42:14', 'create', '2025-09-11 20:42:14', '2025-09-11 20:42:14');
INSERT INTO `user_award_record_000` VALUES (31, 'gs2m5680', 100301, 100006, '820604927800', 104, '小米台灯', '2025-09-11 12:42:19', 'create', '2025-09-11 20:42:19', '2025-09-11 20:42:19');
INSERT INTO `user_award_record_000` VALUES (32, 'hm1i2v_sp2', 100301, 100006, '166150736638', 101, '随机积分', '2025-09-11 12:42:22', 'completed', '2025-09-11 20:42:21', '2025-09-11 20:42:21');
INSERT INTO `user_award_record_000` VALUES (33, 'k5mmcx.jew', 100301, 100006, '844827963310', 108, '暴走玩偶', '2025-09-11 12:42:22', 'create', '2025-09-11 20:42:22', '2025-09-11 20:42:22');
INSERT INTO `user_award_record_000` VALUES (34, 'tbfnuy40', 100301, 100006, '844816569711', 102, 'OpenAI会员卡', '2025-09-11 12:42:26', 'create', '2025-09-11 20:42:26', '2025-09-11 20:42:26');
INSERT INTO `user_award_record_000` VALUES (35, 'gs2m56_tqo', 100301, 100006, '681436236302', 108, '暴走玩偶', '2025-09-11 12:42:28', 'create', '2025-09-11 20:42:27', '2025-09-11 20:42:27');
INSERT INTO `user_award_record_000` VALUES (36, 'me4hce.nyg4', 100301, 100006, '164505502318', 102, 'OpenAI会员卡', '2025-09-11 12:42:30', 'create', '2025-09-11 20:42:30', '2025-09-11 20:42:30');
INSERT INTO `user_award_record_000` VALUES (37, 'k8qkfk_ffg84', 100301, 100006, '475083682487', 102, 'OpenAI会员卡', '2025-09-11 12:42:34', 'create', '2025-09-11 20:42:34', '2025-09-11 20:42:34');
INSERT INTO `user_award_record_000` VALUES (38, 'lm1sbt6', 100301, 100006, '586238808337', 101, '随机积分', '2025-09-11 12:42:36', 'completed', '2025-09-11 20:42:36', '2025-09-11 20:42:36');
INSERT INTO `user_award_record_000` VALUES (39, 'po4sf0_ib777', 100301, 100006, '815868729171', 101, '随机积分', '2025-09-11 12:42:38', 'completed', '2025-09-11 20:42:37', '2025-09-11 20:42:37');
INSERT INTO `user_award_record_000` VALUES (40, 'lmfmhh.ire77', 100301, 100006, '803199541856', 104, '小米台灯', '2025-09-11 12:42:38', 'create', '2025-09-11 20:42:38', '2025-09-11 20:42:38');
INSERT INTO `user_award_record_000` VALUES (41, 'ucop8d.glk', 100301, 100006, '086628206446', 101, '随机积分', '2025-09-11 12:42:44', 'completed', '2025-09-11 20:42:44', '2025-09-11 20:42:44');
INSERT INTO `user_award_record_000` VALUES (42, 'ryilj186', 100301, 100006, '018611415498', 104, '小米台灯', '2025-09-11 12:42:45', 'create', '2025-09-11 20:42:45', '2025-09-11 20:42:45');
INSERT INTO `user_award_record_000` VALUES (43, 'k7ngvs_gbf', 100301, 100006, '919983311697', 102, 'OpenAI会员卡', '2025-09-11 12:42:48', 'create', '2025-09-11 20:42:48', '2025-09-11 20:42:48');
INSERT INTO `user_award_record_000` VALUES (44, 'gs2m56_ggl72', 100301, 100006, '846235838309', 104, '小米台灯', '2025-09-11 12:42:54', 'create', '2025-09-11 20:42:54', '2025-09-11 20:42:54');
INSERT INTO `user_award_record_000` VALUES (45, 'g6nmzi_irk', 100301, 100006, '277454856421', 104, '小米台灯', '2025-09-11 12:43:01', 'create', '2025-09-11 20:43:00', '2025-09-11 20:43:00');
INSERT INTO `user_award_record_000` VALUES (46, 'mzitue_nj340', 100301, 100006, '906419520345', 107, '小霸王游戏机', '2025-09-11 12:43:04', 'create', '2025-09-11 20:43:04', '2025-09-11 20:43:04');
INSERT INTO `user_award_record_000` VALUES (47, 'k8qi48_tml1', 100301, 100006, '811233753965', 101, '随机积分', '2025-09-11 12:43:06', 'completed', '2025-09-11 20:43:05', '2025-09-11 20:43:06');
INSERT INTO `user_award_record_000` VALUES (48, 'k18iui_tn253', 100301, 100006, '184983286687', 108, '暴走玩偶', '2025-09-11 12:43:11', 'create', '2025-09-11 20:43:11', '2025-09-11 20:43:11');
INSERT INTO `user_award_record_000` VALUES (49, 'rydkjv_qj262', 100301, 100006, '231057891225', 107, '小霸王游戏机', '2025-09-11 12:43:13', 'create', '2025-09-11 20:43:13', '2025-09-11 20:43:13');
INSERT INTO `user_award_record_000` VALUES (50, 'v9rmdm75', 100301, 100006, '822067077994', 104, '小米台灯', '2025-09-11 12:43:15', 'create', '2025-09-11 20:43:14', '2025-09-11 20:43:14');
INSERT INTO `user_award_record_000` VALUES (51, 'hm1nuy_tt6', 100301, 100006, '542128510344', 101, '随机积分', '2025-09-11 12:43:15', 'completed', '2025-09-11 20:43:15', '2025-09-11 20:43:15');
INSERT INTO `user_award_record_000` VALUES (52, 'ppm_sks30', 100301, 100006, '603113690564', 101, '随机积分', '2025-09-11 12:43:18', 'completed', '2025-09-11 20:43:17', '2025-09-11 20:43:17');
INSERT INTO `user_award_record_000` VALUES (53, 'ffxlmf_lrs54', 100301, 100006, '960946217886', 101, '随机积分', '2025-09-11 12:43:19', 'completed', '2025-09-11 20:43:19', '2025-09-11 20:43:19');
INSERT INTO `user_award_record_000` VALUES (54, 'lh7k1z_i46', 100301, 100006, '803498049674', 107, '小霸王游戏机', '2025-09-11 12:43:23', 'create', '2025-09-11 20:43:22', '2025-09-11 20:43:22');
INSERT INTO `user_award_record_000` VALUES (55, 'gs2k34_u4f88', 100301, 100006, '126225669348', 104, '小米台灯', '2025-09-11 12:43:31', 'create', '2025-09-11 20:43:30', '2025-09-11 20:43:30');
INSERT INTO `user_award_record_000` VALUES (56, 'k5mmcx.qg973', 100301, 100006, '821081860164', 107, '小霸王游戏机', '2025-09-11 12:43:33', 'create', '2025-09-11 20:43:32', '2025-09-11 20:43:32');
INSERT INTO `user_award_record_000` VALUES (57, 'ffxlmf_gqg33', 100301, 100006, '083921769796', 104, '小米台灯', '2025-09-11 12:43:35', 'create', '2025-09-11 20:43:35', '2025-09-11 20:43:35');
INSERT INTO `user_award_record_000` VALUES (58, 'k18iui23', 100301, 100006, '626574273186', 104, '小米台灯', '2025-09-11 12:43:40', 'create', '2025-09-11 20:43:39', '2025-09-11 20:43:39');
INSERT INTO `user_award_record_000` VALUES (59, 'jlbffg20', 100301, 100006, '323886762199', 102, 'OpenAI会员卡', '2025-09-11 12:43:44', 'create', '2025-09-11 20:43:43', '2025-09-11 20:43:43');

-- ----------------------------
-- Table structure for user_award_record_001
-- ----------------------------
DROP TABLE IF EXISTS `user_award_record_001`;
CREATE TABLE `user_award_record_001`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `strategy_id` bigint NOT NULL COMMENT '抽奖策略ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '抽奖订单ID【作为幂等使用】',
  `award_id` int NOT NULL COMMENT '奖品ID',
  `award_title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '奖品标题（名称）',
  `award_time` datetime NOT NULL COMMENT '中奖时间',
  `award_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'create' COMMENT '奖品状态；create-创建、completed-发奖完成',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_activity_id`(`activity_id` ASC) USING BTREE,
  INDEX `idx_award_id`(`strategy_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 251 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户中奖记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_award_record_001
-- ----------------------------
INSERT INTO `user_award_record_001` VALUES (161, 'fvpki7.keb22', 100301, 100006, '152137500941', 101, '随机积分', '2025-09-11 12:22:02', 'completed', '2025-09-11 20:22:02', '2025-09-11 20:22:02');
INSERT INTO `user_award_record_001` VALUES (162, 'fvpki7.keb22', 100301, 100006, '605504311002', 101, '随机积分', '2025-09-11 12:22:09', 'completed', '2025-09-11 20:22:09', '2025-09-11 20:22:09');
INSERT INTO `user_award_record_001` VALUES (163, 'fvpki7.keb22', 100301, 100006, '450114916627', 107, '小霸王游戏机', '2025-09-11 12:22:14', 'create', '2025-09-11 20:22:14', '2025-09-11 20:22:14');
INSERT INTO `user_award_record_001` VALUES (164, 'fvpki7.keb22', 100301, 100006, '912745270715', 101, '随机积分', '2025-09-11 12:37:05', 'completed', '2025-09-11 20:37:04', '2025-09-11 20:37:05');
INSERT INTO `user_award_record_001` VALUES (167, 'fvpki7.keb22', 100301, 100006, '749956198335', 106, '轻奢办公椅', '2025-09-11 12:37:06', 'create', '2025-09-11 20:37:06', '2025-09-11 20:37:06');
INSERT INTO `user_award_record_001` VALUES (169, 'fvpki7.keb22', 100301, 100006, '040211232269', 102, 'OpenAI会员卡', '2025-09-11 12:37:06', 'create', '2025-09-11 20:37:06', '2025-09-11 20:37:06');
INSERT INTO `user_award_record_001` VALUES (170, 'v9bmdm98', 100301, 100006, '736666802256', 101, '随机积分', '2025-09-11 12:37:06', 'completed', '2025-09-11 20:37:06', '2025-09-11 20:37:06');
INSERT INTO `user_award_record_001` VALUES (172, 'fvpki7.keb22', 100301, 100006, '245170135024', 101, '随机积分', '2025-09-11 12:37:08', 'completed', '2025-09-11 20:37:07', '2025-09-11 20:37:07');
INSERT INTO `user_award_record_001` VALUES (174, 'tbfv9r_l7q84', 100301, 100006, '417206744050', 101, '随机积分', '2025-09-11 12:37:08', 'completed', '2025-09-11 20:37:08', '2025-09-11 20:37:08');
INSERT INTO `user_award_record_001` VALUES (175, 'jwupu5.gjv', 100301, 100006, '133003370970', 101, '随机积分', '2025-09-11 12:37:09', 'completed', '2025-09-11 20:37:08', '2025-09-11 20:37:08');
INSERT INTO `user_award_record_001` VALUES (178, 'v9bmdm98', 100301, 100006, '110775777610', 107, '小霸王游戏机', '2025-09-11 12:37:09', 'create', '2025-09-11 20:37:09', '2025-09-11 20:37:09');
INSERT INTO `user_award_record_001` VALUES (179, 'fvpki7.keb22', 100301, 100006, '892818350905', 103, '支付优惠券', '2025-09-11 12:40:33', 'create', '2025-09-11 20:40:33', '2025-09-11 20:40:33');
INSERT INTO `user_award_record_001` VALUES (180, 'v9bmdm98', 100301, 100006, '356166292511', 102, 'OpenAI会员卡', '2025-09-11 12:40:35', 'create', '2025-09-11 20:40:34', '2025-09-11 20:40:34');
INSERT INTO `user_award_record_001` VALUES (181, 'tbfv9r_l7q84', 100301, 100006, '225304149056', 106, '轻奢办公椅', '2025-09-11 12:40:36', 'create', '2025-09-11 20:40:35', '2025-09-11 20:40:35');
INSERT INTO `user_award_record_001` VALUES (182, 'jwupu5.gjv', 100301, 100006, '752808039912', 107, '小霸王游戏机', '2025-09-11 12:40:37', 'create', '2025-09-11 20:40:36', '2025-09-11 20:40:36');
INSERT INTO `user_award_record_001` VALUES (183, 'k1zsbt76', 100301, 100006, '756591068309', 101, '随机积分', '2025-09-11 12:40:41', 'completed', '2025-09-11 20:40:40', '2025-09-11 20:40:41');
INSERT INTO `user_award_record_001` VALUES (184, 'tgmo3v_tg9', 100301, 100006, '670748640780', 101, '随机积分', '2025-09-11 12:40:46', 'completed', '2025-09-11 20:40:46', '2025-09-11 20:40:46');
INSERT INTO `user_award_record_001` VALUES (185, 'sf0ppm47', 100301, 100006, '871712532191', 103, '支付优惠券', '2025-09-11 12:40:47', 'create', '2025-09-11 20:40:47', '2025-09-11 20:40:47');
INSERT INTO `user_award_record_001` VALUES (186, 'jzzgm341', 100301, 100006, '966825698801', 101, '随机积分', '2025-09-11 12:40:49', 'completed', '2025-09-11 20:40:49', '2025-09-11 20:40:49');
INSERT INTO `user_award_record_001` VALUES (187, 'i2xmy224', 100301, 100006, '713708837505', 101, '随机积分', '2025-09-11 12:40:50', 'completed', '2025-09-11 20:40:50', '2025-09-11 20:40:50');
INSERT INTO `user_award_record_001` VALUES (188, 'nlrlqi_j7y', 100301, 100006, '313538968950', 103, '支付优惠券', '2025-09-11 12:40:51', 'create', '2025-09-11 20:40:51', '2025-09-11 20:40:51');
INSERT INTO `user_award_record_001` VALUES (189, 'ryilj149', 100301, 100006, '148928701524', 101, '随机积分', '2025-09-11 12:40:52', 'completed', '2025-09-11 20:40:51', '2025-09-11 20:40:51');
INSERT INTO `user_award_record_001` VALUES (190, 'ixjlj1_tqo', 100301, 100006, '871633686147', 104, '小米台灯', '2025-09-11 12:40:53', 'create', '2025-09-11 20:40:53', '2025-09-11 20:40:53');
INSERT INTO `user_award_record_001` VALUES (191, 'ui8_n1l68', 100301, 100006, '063523150482', 103, '支付优惠券', '2025-09-11 12:40:56', 'create', '2025-09-11 20:40:55', '2025-09-11 20:40:55');
INSERT INTO `user_award_record_001` VALUES (192, 'me4nzu_ph932', 100301, 100006, '649329124724', 103, '支付优惠券', '2025-09-11 12:41:08', 'create', '2025-09-11 20:41:08', '2025-09-11 20:41:08');
INSERT INTO `user_award_record_001` VALUES (193, 'tbfv9r_ssq', 100301, 100006, '806787329592', 104, '小米台灯', '2025-09-11 12:41:09', 'create', '2025-09-11 20:41:08', '2025-09-11 20:41:08');
INSERT INTO `user_award_record_001` VALUES (194, 'pu9ggq.gc3', 100301, 100006, '698154511241', 103, '支付优惠券', '2025-09-11 12:41:11', 'create', '2025-09-11 20:41:11', '2025-09-11 20:41:11');
INSERT INTO `user_award_record_001` VALUES (195, 'rmftt492', 100301, 100006, '392430616332', 102, 'OpenAI会员卡', '2025-09-11 12:41:12', 'create', '2025-09-11 20:41:12', '2025-09-11 20:41:12');
INSERT INTO `user_award_record_001` VALUES (196, 'ui859', 100301, 100006, '495875507119', 103, '支付优惠券', '2025-09-11 12:41:13', 'create', '2025-09-11 20:41:13', '2025-09-11 20:41:13');
INSERT INTO `user_award_record_001` VALUES (197, 'hm1nuy43', 100301, 100006, '530579532664', 108, '暴走玩偶', '2025-09-11 12:41:15', 'create', '2025-09-11 20:41:15', '2025-09-11 20:41:15');
INSERT INTO `user_award_record_001` VALUES (198, 'nlrlqi66', 100301, 100006, '771204990342', 104, '小米台灯', '2025-09-11 12:41:16', 'create', '2025-09-11 20:41:15', '2025-09-11 20:41:15');
INSERT INTO `user_award_record_001` VALUES (199, 'tgmo3v.tfa99', 100301, 100006, '482088172491', 104, '小米台灯', '2025-09-11 12:41:25', 'create', '2025-09-11 20:41:25', '2025-09-11 20:41:25');
INSERT INTO `user_award_record_001` VALUES (200, 'fozmy264', 100301, 100006, '579074268560', 104, '小米台灯', '2025-09-11 12:41:33', 'create', '2025-09-11 20:41:32', '2025-09-11 20:41:32');
INSERT INTO `user_award_record_001` VALUES (201, 'fgwuih10', 100301, 100006, '044003661381', 102, 'OpenAI会员卡', '2025-09-11 12:41:34', 'create', '2025-09-11 20:41:34', '2025-09-11 20:41:34');
INSERT INTO `user_award_record_001` VALUES (202, 'gs2m5634', 100301, 100006, '811524536799', 102, 'OpenAI会员卡', '2025-09-11 12:41:35', 'create', '2025-09-11 20:41:35', '2025-09-11 20:41:35');
INSERT INTO `user_award_record_001` VALUES (203, 'jzzgm374', 100301, 100006, '495836124725', 101, '随机积分', '2025-09-11 12:41:40', 'completed', '2025-09-11 20:41:39', '2025-09-11 20:41:39');
INSERT INTO `user_award_record_001` VALUES (204, 'i1cuji.nev32', 100301, 100006, '225776565928', 104, '小米台灯', '2025-09-11 12:41:41', 'create', '2025-09-11 20:41:40', '2025-09-11 20:41:40');
INSERT INTO `user_award_record_001` VALUES (205, 'hm1nuy.lqd', 100301, 100006, '782987856019', 103, '支付优惠券', '2025-09-11 12:41:42', 'create', '2025-09-11 20:41:42', '2025-09-11 20:41:42');
INSERT INTO `user_award_record_001` VALUES (206, 'k1zggq.o34', 100301, 100006, '852715642756', 101, '随机积分', '2025-09-11 12:41:43', 'completed', '2025-09-11 20:41:43', '2025-09-11 20:41:43');
INSERT INTO `user_award_record_001` VALUES (207, 'fextfv_hqj', 100301, 100006, '940049978778', 101, '随机积分', '2025-09-11 12:41:46', 'completed', '2025-09-11 20:41:46', '2025-09-11 20:41:46');
INSERT INTO `user_award_record_001` VALUES (208, 'iuk.ptd72', 100301, 100006, '219005873316', 107, '小霸王游戏机', '2025-09-11 12:41:48', 'create', '2025-09-11 20:41:47', '2025-09-11 20:41:47');
INSERT INTO `user_award_record_001` VALUES (209, 'p72k3d43', 100301, 100006, '921294224927', 102, 'OpenAI会员卡', '2025-09-11 12:41:50', 'create', '2025-09-11 20:41:50', '2025-09-11 20:41:50');
INSERT INTO `user_award_record_001` VALUES (210, 'gztmft22', 100301, 100006, '769143912994', 101, '随机积分', '2025-09-11 12:41:59', 'completed', '2025-09-11 20:41:59', '2025-09-11 20:41:59');
INSERT INTO `user_award_record_001` VALUES (211, 'oq3kgn_hge', 100301, 100006, '385794861024', 101, '随机积分', '2025-09-11 12:42:01', 'completed', '2025-09-11 20:42:01', '2025-09-11 20:42:01');
INSERT INTO `user_award_record_001` VALUES (212, 'gs2m5656', 100301, 100006, '908488826853', 101, '随机积分', '2025-09-11 12:42:03', 'completed', '2025-09-11 20:42:03', '2025-09-11 20:42:03');
INSERT INTO `user_award_record_001` VALUES (213, 'ucop8d_i4y87', 100301, 100006, '204482846618', 107, '小霸王游戏机', '2025-09-11 12:42:05', 'create', '2025-09-11 20:42:04', '2025-09-11 20:42:04');
INSERT INTO `user_award_record_001` VALUES (214, 'k88to334', 100301, 100006, '447012892329', 102, 'OpenAI会员卡', '2025-09-11 12:42:07', 'create', '2025-09-11 20:42:07', '2025-09-11 20:42:07');
INSERT INTO `user_award_record_001` VALUES (215, 'lm1sds_rkx', 100301, 100006, '540735345140', 101, '随机积分', '2025-09-11 12:42:11', 'completed', '2025-09-11 20:42:10', '2025-09-11 20:42:10');
INSERT INTO `user_award_record_001` VALUES (216, 'o97scp_i0k11', 100301, 100006, '371400129964', 107, '小霸王游戏机', '2025-09-11 12:42:18', 'create', '2025-09-11 20:42:17', '2025-09-11 20:42:17');
INSERT INTO `user_award_record_001` VALUES (217, 'k7znlr.s3r', 100301, 100006, '635597877347', 103, '支付优惠券', '2025-09-11 12:42:19', 'create', '2025-09-11 20:42:18', '2025-09-11 20:42:18');
INSERT INTO `user_award_record_001` VALUES (218, 'fmni4832', 100301, 100006, '224937399918', 107, '小霸王游戏机', '2025-09-11 12:42:20', 'create', '2025-09-11 20:42:20', '2025-09-11 20:42:20');
INSERT INTO `user_award_record_001` VALUES (219, 'o97scp87', 100301, 100006, '224062467529', 104, '小米台灯', '2025-09-11 12:42:21', 'create', '2025-09-11 20:42:21', '2025-09-11 20:42:21');
INSERT INTO `user_award_record_001` VALUES (220, 'tdcsbt.sbq1', 100301, 100006, '862019663400', 101, '随机积分', '2025-09-11 12:42:23', 'completed', '2025-09-11 20:42:23', '2025-09-11 20:42:23');
INSERT INTO `user_award_record_001` VALUES (221, 'fvpki7.ghc52', 100301, 100006, '521007419974', 102, 'OpenAI会员卡', '2025-09-11 12:42:25', 'create', '2025-09-11 20:42:25', '2025-09-11 20:42:25');
INSERT INTO `user_award_record_001` VALUES (222, 'gs2k34_i3h8', 100301, 100006, '338432256895', 108, '暴走玩偶', '2025-09-11 12:42:29', 'create', '2025-09-11 20:42:28', '2025-09-11 20:42:28');
INSERT INTO `user_award_record_001` VALUES (223, 'mzitue.o2g87', 100301, 100006, '393834428885', 101, '随机积分', '2025-09-11 12:42:31', 'completed', '2025-09-11 20:42:31', '2025-09-11 20:42:31');
INSERT INTO `user_award_record_001` VALUES (224, 'ggqryd44', 100301, 100006, '756770617554', 104, '小米台灯', '2025-09-11 12:42:42', 'create', '2025-09-11 20:42:42', '2025-09-11 20:42:42');
INSERT INTO `user_award_record_001` VALUES (225, 'tt6kfy.g4v78', 100301, 100006, '453671620406', 104, '小米台灯', '2025-09-11 12:42:44', 'create', '2025-09-11 20:42:43', '2025-09-11 20:42:43');
INSERT INTO `user_award_record_001` VALUES (226, 'o97fmn21', 100301, 100006, '816061191703', 107, '小霸王游戏机', '2025-09-11 12:42:47', 'create', '2025-09-11 20:42:47', '2025-09-11 20:42:47');
INSERT INTO `user_award_record_001` VALUES (227, 'i1cuji52', 100301, 100006, '120726740982', 101, '随机积分', '2025-09-11 12:42:50', 'completed', '2025-09-11 20:42:49', '2025-09-11 20:42:49');
INSERT INTO `user_award_record_001` VALUES (228, 'lmfmhh_sl5', 100301, 100006, '033518193799', 103, '支付优惠券', '2025-09-11 12:42:51', 'create', '2025-09-11 20:42:51', '2025-09-11 20:42:51');
INSERT INTO `user_award_record_001` VALUES (229, 'fevfoz.lpi22', 100301, 100006, '351834030681', 104, '小米台灯', '2025-09-11 12:42:54', 'create', '2025-09-11 20:42:53', '2025-09-11 20:42:53');
INSERT INTO `user_award_record_001` VALUES (230, 'izhpd6_iwd81', 100301, 100006, '052387100699', 107, '小霸王游戏机', '2025-09-11 12:42:58', 'create', '2025-09-11 20:42:57', '2025-09-11 20:42:57');
INSERT INTO `user_award_record_001` VALUES (231, 'jcvsbt.fh869', 100301, 100006, '104174547688', 107, '小霸王游戏机', '2025-09-11 12:42:59', 'create', '2025-09-11 20:42:58', '2025-09-11 20:42:58');
INSERT INTO `user_award_record_001` VALUES (232, 'o97kgs_ghb18', 100301, 100006, '403554336722', 101, '随机积分', '2025-09-11 12:43:00', 'completed', '2025-09-11 20:43:00', '2025-09-11 20:43:00');
INSERT INTO `user_award_record_001` VALUES (233, 'tw6my276', 100301, 100006, '071068426153', 101, '随机积分', '2025-09-11 12:43:03', 'completed', '2025-09-11 20:43:03', '2025-09-11 20:43:03');
INSERT INTO `user_award_record_001` VALUES (234, 'n0emzg_tn27', 100301, 100006, '503860844003', 101, '随机积分', '2025-09-11 12:43:08', 'completed', '2025-09-11 20:43:08', '2025-09-11 20:43:08');
INSERT INTO `user_award_record_001` VALUES (235, 'k1zsbt_l9x91', 100301, 100006, '075525008239', 101, '随机积分', '2025-09-11 12:43:09', 'completed', '2025-09-11 20:43:09', '2025-09-11 20:43:09');
INSERT INTO `user_award_record_001` VALUES (236, 'k18iui_ist', 100301, 100006, '462978645860', 104, '小米台灯', '2025-09-11 12:43:12', 'create', '2025-09-11 20:43:12', '2025-09-11 20:43:12');
INSERT INTO `user_award_record_001` VALUES (237, 'i1cl5v.ioq82', 100301, 100006, '523223098524', 101, '随机积分', '2025-09-11 12:43:14', 'completed', '2025-09-11 20:43:13', '2025-09-11 20:43:13');
INSERT INTO `user_award_record_001` VALUES (238, 'ts5hvb_ssq', 100301, 100006, '852348661153', 102, 'OpenAI会员卡', '2025-09-11 12:43:21', 'create', '2025-09-11 20:43:21', '2025-09-11 20:43:21');
INSERT INTO `user_award_record_001` VALUES (239, 'tw9izl_puf', 100301, 100006, '000636486649', 101, '随机积分', '2025-09-11 12:43:28', 'completed', '2025-09-11 20:43:27', '2025-09-11 20:43:27');
INSERT INTO `user_award_record_001` VALUES (240, 'i1cvdk.sk766', 100301, 100006, '567337063279', 101, '随机积分', '2025-09-11 12:43:29', 'completed', '2025-09-11 20:43:28', '2025-09-11 20:43:28');
INSERT INTO `user_award_record_001` VALUES (241, 'oq3kgn.gl4o6y10', 100301, 100006, '315521815970', 101, '随机积分', '2025-09-11 12:43:33', 'completed', '2025-09-11 20:43:33', '2025-09-11 20:43:33');
INSERT INTO `user_award_record_001` VALUES (242, 'lm1i2v63', 100301, 100006, '604611442316', 101, '随机积分', '2025-09-11 12:43:36', 'completed', '2025-09-11 20:43:36', '2025-09-11 20:43:36');
INSERT INTO `user_award_record_001` VALUES (243, 'oq3kgn39', 100301, 100006, '549524966738', 101, '随机积分', '2025-09-11 12:43:38', 'completed', '2025-09-11 20:43:38', '2025-09-11 20:43:38');
INSERT INTO `user_award_record_001` VALUES (244, 'k1zu0c_skj34', 100301, 100006, '040699385942', 104, '小米台灯', '2025-09-11 12:43:43', 'create', '2025-09-11 20:43:42', '2025-09-11 20:43:42');
INSERT INTO `user_award_record_001` VALUES (245, 'v9bmdm.s3r72', 100301, 100006, '152214386602', 108, '暴走玩偶', '2025-09-11 12:43:45', 'create', '2025-09-11 20:43:45', '2025-09-11 20:43:45');
INSERT INTO `user_award_record_001` VALUES (246, 'sf0ppm_onw', 100301, 100006, '353513340001', 101, '随机积分', '2025-09-11 12:43:49', 'completed', '2025-09-11 20:43:49', '2025-09-11 20:43:49');
INSERT INTO `user_award_record_001` VALUES (247, 'u6mgge_sfi', 100301, 100006, '522475732741', 107, '小霸王游戏机', '2025-09-11 12:43:50', 'create', '2025-09-11 20:43:50', '2025-09-11 20:43:50');
INSERT INTO `user_award_record_001` VALUES (248, 'hm1hs7_fep', 100301, 100006, '094317938876', 101, '随机积分', '2025-09-11 12:43:51', 'completed', '2025-09-11 20:43:50', '2025-09-11 20:43:51');
INSERT INTO `user_award_record_001` VALUES (249, 'mzitue_lzw', 100301, 100006, '513797830139', 101, '随机积分', '2025-09-11 12:43:52', 'completed', '2025-09-11 20:43:51', '2025-09-11 20:43:51');
INSERT INTO `user_award_record_001` VALUES (250, 'me4nzu15', 100301, 100006, '618605856813', 108, '暴走玩偶', '2025-09-11 12:43:54', 'create', '2025-09-11 20:43:54', '2025-09-11 20:43:54');

-- ----------------------------
-- Table structure for user_award_record_002
-- ----------------------------
DROP TABLE IF EXISTS `user_award_record_002`;
CREATE TABLE `user_award_record_002`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `strategy_id` bigint NOT NULL COMMENT '抽奖策略ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '抽奖订单ID【作为幂等使用】',
  `award_id` int NOT NULL COMMENT '奖品ID',
  `award_title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '奖品标题（名称）',
  `award_time` datetime NOT NULL COMMENT '中奖时间',
  `award_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'create' COMMENT '奖品状态；create-创建、completed-发奖完成',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_activity_id`(`activity_id` ASC) USING BTREE,
  INDEX `idx_award_id`(`strategy_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 95 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户中奖记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_award_record_002
-- ----------------------------
INSERT INTO `user_award_record_002` VALUES (17, 'ixjlj1_fgs', 100301, 100006, '485358948528', 101, '随机积分', '2025-09-11 12:37:06', 'completed', '2025-09-11 20:37:05', '2025-09-11 20:37:05');
INSERT INTO `user_award_record_002` VALUES (18, 'ixjlj1_fgs', 100301, 100006, '548043642799', 101, '随机积分', '2025-09-11 12:37:08', 'completed', '2025-09-11 20:37:07', '2025-09-11 20:37:08');
INSERT INTO `user_award_record_002` VALUES (19, 'ixjlj1_fgs', 100301, 100006, '532429947610', 101, '随机积分', '2025-09-11 12:37:08', 'completed', '2025-09-11 20:37:08', '2025-09-11 20:37:08');
INSERT INTO `user_award_record_002` VALUES (20, 'ixjlj1_fgs', 100301, 100006, '524147420844', 103, '支付优惠券', '2025-09-11 12:37:08', 'create', '2025-09-11 20:37:08', '2025-09-11 20:37:08');
INSERT INTO `user_award_record_002` VALUES (21, 'ixjlj1_fgs', 100301, 100006, '138453371004', 106, '轻奢办公椅', '2025-09-11 12:37:08', 'create', '2025-09-11 20:37:08', '2025-09-11 20:37:08');
INSERT INTO `user_award_record_002` VALUES (22, 'ixjlj1_fgs', 100301, 100006, '112183785106', 106, '轻奢办公椅', '2025-09-11 12:37:08', 'create', '2025-09-11 20:37:08', '2025-09-11 20:37:08');
INSERT INTO `user_award_record_002` VALUES (23, 'ixjlj1_fgs', 100301, 100006, '499395995020', 102, 'OpenAI会员卡', '2025-09-11 12:40:34', 'create', '2025-09-11 20:40:33', '2025-09-11 20:40:33');
INSERT INTO `user_award_record_002` VALUES (24, 'izhfoz52', 100301, 100006, '256087726548', 102, 'OpenAI会员卡', '2025-09-11 12:40:39', 'create', '2025-09-11 20:40:38', '2025-09-11 20:40:38');
INSERT INTO `user_award_record_002` VALUES (25, 'jlbi46_jew', 100301, 100006, '178433616575', 101, '随机积分', '2025-09-11 12:40:39', 'completed', '2025-09-11 20:40:39', '2025-09-11 20:40:39');
INSERT INTO `user_award_record_002` VALUES (26, 'vdak5x51', 100301, 100006, '221950821442', 103, '支付优惠券', '2025-09-11 12:40:40', 'create', '2025-09-11 20:40:40', '2025-09-11 20:40:40');
INSERT INTO `user_award_record_002` VALUES (27, 'fsekfk.jh485', 100301, 100006, '777944065189', 101, '随机积分', '2025-09-11 12:40:43', 'completed', '2025-09-11 20:40:42', '2025-09-11 20:40:43');
INSERT INTO `user_award_record_002` VALUES (28, 'pd6fvp_kd762', 100301, 100006, '690852255757', 102, 'OpenAI会员卡', '2025-09-11 12:40:44', 'create', '2025-09-11 20:40:44', '2025-09-11 20:40:44');
INSERT INTO `user_award_record_002` VALUES (29, 'k8qscp_glx72', 100301, 100006, '972070713546', 108, '暴走玩偶', '2025-09-11 12:40:50', 'create', '2025-09-11 20:40:49', '2025-09-11 20:40:49');
INSERT INTO `user_award_record_002` VALUES (30, 'm97k5q.ny968', 100301, 100006, '322136064600', 101, '随机积分', '2025-09-11 12:40:55', 'completed', '2025-09-11 20:40:55', '2025-09-11 20:40:55');
INSERT INTO `user_award_record_002` VALUES (31, 'k1z74', 100301, 100006, '017668200517', 103, '支付优惠券', '2025-09-11 12:40:57', 'create', '2025-09-11 20:40:57', '2025-09-11 20:40:57');
INSERT INTO `user_award_record_002` VALUES (32, 'i1csbt_gqo91', 100301, 100006, '650808405297', 107, '小霸王游戏机', '2025-09-11 12:40:59', 'create', '2025-09-11 20:40:59', '2025-09-11 20:40:59');
INSERT INTO `user_award_record_002` VALUES (33, 'ts5hvb.skx', 100301, 100006, '531965810993', 101, '随机积分', '2025-09-11 12:41:00', 'completed', '2025-09-11 20:40:59', '2025-09-11 20:40:59');
INSERT INTO `user_award_record_002` VALUES (34, 'm8eiuk_fl0', 100301, 100006, '140757662837', 108, '暴走玩偶', '2025-09-11 12:41:00', 'create', '2025-09-11 20:41:00', '2025-09-11 20:41:00');
INSERT INTO `user_award_record_002` VALUES (35, 'k8qkfk.sks1', 100301, 100006, '161973468187', 102, 'OpenAI会员卡', '2025-09-11 12:41:01', 'create', '2025-09-11 20:41:01', '2025-09-11 20:41:01');
INSERT INTO `user_award_record_002` VALUES (36, 'ui865', 100301, 100006, '807771818632', 103, '支付优惠券', '2025-09-11 12:41:02', 'create', '2025-09-11 20:41:01', '2025-09-11 20:41:01');
INSERT INTO `user_award_record_002` VALUES (37, 'tw6my286', 100301, 100006, '458886356058', 101, '随机积分', '2025-09-11 12:41:04', 'completed', '2025-09-11 20:41:04', '2025-09-11 20:41:04');
INSERT INTO `user_award_record_002` VALUES (38, 'tt6kfy.i809', 100301, 100006, '501392725475', 103, '支付优惠券', '2025-09-11 12:41:05', 'create', '2025-09-11 20:41:05', '2025-09-11 20:41:05');
INSERT INTO `user_award_record_002` VALUES (39, 'k5mhm1_ky2', 100301, 100006, '478297588215', 108, '暴走玩偶', '2025-09-11 12:41:10', 'create', '2025-09-11 20:41:10', '2025-09-11 20:41:10');
INSERT INTO `user_award_record_002` VALUES (40, 'tt6kfy.lly', 100301, 100006, '583573290486', 104, '小米台灯', '2025-09-11 12:41:13', 'create', '2025-09-11 20:41:12', '2025-09-11 20:41:12');
INSERT INTO `user_award_record_002` VALUES (41, 'fevfoz.gne23', 100301, 100006, '617071776299', 104, '小米台灯', '2025-09-11 12:41:17', 'create', '2025-09-11 20:41:16', '2025-09-11 20:41:16');
INSERT INTO `user_award_record_002` VALUES (42, 'mzitue.m2hgkw', 100301, 100006, '206722639797', 101, '随机积分', '2025-09-11 12:41:18', 'completed', '2025-09-11 20:41:18', '2025-09-11 20:41:18');
INSERT INTO `user_award_record_002` VALUES (43, 'pu9ggq_h5q81', 100301, 100006, '973860844310', 103, '支付优惠券', '2025-09-11 12:41:24', 'create', '2025-09-11 20:41:23', '2025-09-11 20:41:23');
INSERT INTO `user_award_record_002` VALUES (44, 'hm1nuy_frz89', 100301, 100006, '730843364839', 107, '小霸王游戏机', '2025-09-11 12:41:25', 'create', '2025-09-11 20:41:24', '2025-09-11 20:41:24');
INSERT INTO `user_award_record_002` VALUES (45, 'k1zu0c.gbf58', 100301, 100006, '521343929271', 103, '支付优惠券', '2025-09-11 12:41:26', 'create', '2025-09-11 20:41:26', '2025-09-11 20:41:26');
INSERT INTO `user_award_record_002` VALUES (46, 'id7mft99', 100301, 100006, '854020727435', 108, '暴走玩偶', '2025-09-11 12:41:27', 'create', '2025-09-11 20:41:27', '2025-09-11 20:41:27');
INSERT INTO `user_award_record_002` VALUES (47, 'fmnrmi_ptd', 100301, 100006, '084327458304', 102, 'OpenAI会员卡', '2025-09-11 12:41:29', 'create', '2025-09-11 20:41:29', '2025-09-11 20:41:29');
INSERT INTO `user_award_record_002` VALUES (48, 'ttbtw992', 100301, 100006, '617734339171', 101, '随机积分', '2025-09-11 12:41:32', 'completed', '2025-09-11 20:41:31', '2025-09-11 20:41:31');
INSERT INTO `user_award_record_002` VALUES (49, 'mawnuy3', 100301, 100006, '291889375230', 102, 'OpenAI会员卡', '2025-09-11 12:41:36', 'create', '2025-09-11 20:41:35', '2025-09-11 20:41:35');
INSERT INTO `user_award_record_002` VALUES (50, 'izhfoz96', 100301, 100006, '313042747935', 104, '小米台灯', '2025-09-11 12:41:37', 'create', '2025-09-11 20:41:37', '2025-09-11 20:41:37');
INSERT INTO `user_award_record_002` VALUES (51, 'lj1ljf_g8b10', 100301, 100006, '351644873053', 101, '随机积分', '2025-09-11 12:41:41', 'completed', '2025-09-11 20:41:41', '2025-09-11 20:41:41');
INSERT INTO `user_award_record_002` VALUES (52, 'jwui2v_n5e', 100301, 100006, '908380651223', 107, '小霸王游戏机', '2025-09-11 12:41:49', 'create', '2025-09-11 20:41:48', '2025-09-11 20:41:48');
INSERT INTO `user_award_record_002` VALUES (53, 'i1cvdk31', 100301, 100006, '562073825721', 103, '支付优惠券', '2025-09-11 12:41:52', 'create', '2025-09-11 20:41:51', '2025-09-11 20:41:51');
INSERT INTO `user_award_record_002` VALUES (54, 'k5mit6.fk780', 100301, 100006, '615737078424', 108, '暴走玩偶', '2025-09-11 12:41:57', 'create', '2025-09-11 20:41:57', '2025-09-11 20:41:57');
INSERT INTO `user_award_record_002` VALUES (55, 'k5q_io462', 100301, 100006, '485424893384', 107, '小霸王游戏机', '2025-09-11 12:41:58', 'create', '2025-09-11 20:41:58', '2025-09-11 20:41:58');
INSERT INTO `user_award_record_002` VALUES (56, 'fevfoz.kfz23', 100301, 100006, '665781895326', 108, '暴走玩偶', '2025-09-11 12:42:01', 'create', '2025-09-11 20:42:00', '2025-09-11 20:42:00');
INSERT INTO `user_award_record_002` VALUES (57, 'nlrlqi.p1k', 100301, 100006, '757896962943', 107, '小霸王游戏机', '2025-09-11 12:42:04', 'create', '2025-09-11 20:42:04', '2025-09-11 20:42:04');
INSERT INTO `user_award_record_002` VALUES (58, 'ttbtw9.i3e86', 100301, 100006, '488067867408', 101, '随机积分', '2025-09-11 12:42:08', 'completed', '2025-09-11 20:42:08', '2025-09-11 20:42:08');
INSERT INTO `user_award_record_002` VALUES (59, 'knnk88.n5fi8w64', 100301, 100006, '643114980878', 103, '支付优惠券', '2025-09-11 12:42:12', 'create', '2025-09-11 20:42:12', '2025-09-11 20:42:12');
INSERT INTO `user_award_record_002` VALUES (60, 'tt4lj1.fze59', 100301, 100006, '319856419701', 101, '随机积分', '2025-09-11 12:42:13', 'completed', '2025-09-11 20:42:12', '2025-09-11 20:42:13');
INSERT INTO `user_award_record_002` VALUES (61, 'k8qkfk.p9c', 100301, 100006, '419561181762', 101, '随机积分', '2025-09-11 12:42:14', 'completed', '2025-09-11 20:42:13', '2025-09-11 20:42:13');
INSERT INTO `user_award_record_002` VALUES (62, 'ttbtw9_gxn24', 100301, 100006, '716035662307', 101, '随机积分', '2025-09-11 12:42:15', 'completed', '2025-09-11 20:42:15', '2025-09-11 20:42:15');
INSERT INTO `user_award_record_002` VALUES (63, 'fh5q31_irk', 100301, 100006, '763116763002', 103, '支付优惠券', '2025-09-11 12:42:24', 'create', '2025-09-11 20:42:23', '2025-09-11 20:42:23');
INSERT INTO `user_award_record_002` VALUES (64, 'lh7lnf69', 100301, 100006, '526985160531', 102, 'OpenAI会员卡', '2025-09-11 12:42:24', 'create', '2025-09-11 20:42:24', '2025-09-11 20:42:24');
INSERT INTO `user_award_record_002` VALUES (65, 'k5mmcx.n5fi8w', 100301, 100006, '737976416703', 108, '暴走玩偶', '2025-09-11 12:42:27', 'create', '2025-09-11 20:42:27', '2025-09-11 20:42:27');
INSERT INTO `user_award_record_002` VALUES (66, 'k88to3.tqj', 100301, 100006, '115092865111', 107, '小霸王游戏机', '2025-09-11 12:42:32', 'create', '2025-09-11 20:42:31', '2025-09-11 20:42:31');
INSERT INTO `user_award_record_002` VALUES (67, 'lnf_ggu', 100301, 100006, '977866713299', 104, '小米台灯', '2025-09-11 12:42:33', 'create', '2025-09-11 20:42:33', '2025-09-11 20:42:33');
INSERT INTO `user_award_record_002` VALUES (68, 'vacsbt97', 100301, 100006, '349139001591', 102, 'OpenAI会员卡', '2025-09-11 12:42:37', 'create', '2025-09-11 20:42:36', '2025-09-11 20:42:36');
INSERT INTO `user_award_record_002` VALUES (69, 'k88to3_kfc', 100301, 100006, '428683916432', 101, '随机积分', '2025-09-11 12:42:41', 'completed', '2025-09-11 20:42:40', '2025-09-11 20:42:40');
INSERT INTO `user_award_record_002` VALUES (70, 'sf0ppm.k2860', 100301, 100006, '729071627449', 103, '支付优惠券', '2025-09-11 12:42:41', 'create', '2025-09-11 20:42:41', '2025-09-11 20:42:41');
INSERT INTO `user_award_record_002` VALUES (71, 'pybsbt30', 100301, 100006, '171704560181', 103, '支付优惠券', '2025-09-11 12:42:43', 'create', '2025-09-11 20:42:42', '2025-09-11 20:42:42');
INSERT INTO `user_award_record_002` VALUES (72, 'ggqryd_i6hsf355', 100301, 100006, '812926632238', 102, 'OpenAI会员卡', '2025-09-11 12:42:49', 'create', '2025-09-11 20:42:48', '2025-09-11 20:42:48');
INSERT INTO `user_award_record_002` VALUES (73, 'pybsbt85', 100301, 100006, '200778888041', 103, '支付优惠券', '2025-09-11 12:42:50', 'create', '2025-09-11 20:42:50', '2025-09-11 20:42:50');
INSERT INTO `user_award_record_002` VALUES (74, 'ts5tbf24', 100301, 100006, '677289723362', 101, '随机积分', '2025-09-11 12:42:52', 'completed', '2025-09-11 20:42:52', '2025-09-11 20:42:52');
INSERT INTO `user_award_record_002` VALUES (75, 'mzitue_sn143', 100301, 100006, '291153056326', 102, 'OpenAI会员卡', '2025-09-11 12:42:53', 'create', '2025-09-11 20:42:52', '2025-09-11 20:42:52');
INSERT INTO `user_award_record_002` VALUES (76, 'mzitue.tcu0', 100301, 100006, '901646401115', 103, '支付优惠券', '2025-09-11 12:42:55', 'create', '2025-09-11 20:42:55', '2025-09-11 20:42:55');
INSERT INTO `user_award_record_002` VALUES (77, 'izhsf0_go824', 100301, 100006, '275910238047', 104, '小米台灯', '2025-09-11 12:42:56', 'create', '2025-09-11 20:42:56', '2025-09-11 20:42:56');
INSERT INTO `user_award_record_002` VALUES (78, 'lzbmd2.fqn', 100301, 100006, '319761523912', 101, '随机积分', '2025-09-11 12:42:59', 'completed', '2025-09-11 20:42:59', '2025-09-11 20:42:59');
INSERT INTO `user_award_record_002` VALUES (79, 'pd6fvp_ssq', 100301, 100006, '639930962915', 104, '小米台灯', '2025-09-11 12:43:02', 'create', '2025-09-11 20:43:02', '2025-09-11 20:43:02');
INSERT INTO `user_award_record_002` VALUES (80, 'pa8kfk6', 100301, 100006, '812651958318', 103, '支付优惠券', '2025-09-11 12:43:05', 'create', '2025-09-11 20:43:05', '2025-09-11 20:43:05');
INSERT INTO `user_award_record_002` VALUES (81, 'm8eiuk_g3f', 100301, 100006, '501632111531', 108, '暴走玩偶', '2025-09-11 12:43:07', 'create', '2025-09-11 20:43:06', '2025-09-11 20:43:06');
INSERT INTO `user_award_record_002` VALUES (82, 'ktzmzi_scp', 100301, 100006, '017015492394', 101, '随机积分', '2025-09-11 12:43:07', 'completed', '2025-09-11 20:43:07', '2025-09-11 20:43:07');
INSERT INTO `user_award_record_002` VALUES (83, 'k5qsbt.oar93', 100301, 100006, '034321735675', 104, '小米台灯', '2025-09-11 12:43:11', 'create', '2025-09-11 20:43:10', '2025-09-11 20:43:10');
INSERT INTO `user_award_record_002` VALUES (84, 'hm1i2v26', 100301, 100006, '746457778173', 103, '支付优惠券', '2025-09-11 12:43:19', 'create', '2025-09-11 20:43:18', '2025-09-11 20:43:18');
INSERT INTO `user_award_record_002` VALUES (85, 'pybsbt.fnq', 100301, 100006, '595087657700', 103, '支付优惠券', '2025-09-11 12:43:25', 'create', '2025-09-11 20:43:24', '2025-09-11 20:43:24');
INSERT INTO `user_award_record_002` VALUES (86, 'gs2k34_nez', 100301, 100006, '431030825356', 104, '小米台灯', '2025-09-11 12:43:26', 'create', '2025-09-11 20:43:26', '2025-09-11 20:43:26');
INSERT INTO `user_award_record_002` VALUES (87, 'is8k1z_fgs', 100301, 100006, '804363396279', 101, '随机积分', '2025-09-11 12:43:29', 'completed', '2025-09-11 20:43:29', '2025-09-11 20:43:29');
INSERT INTO `user_award_record_002` VALUES (88, 'k7ngvs.gky42', 100301, 100006, '250530534925', 101, '随机积分', '2025-09-11 12:43:34', 'completed', '2025-09-11 20:43:34', '2025-09-11 20:43:34');
INSERT INTO `user_award_record_002` VALUES (89, 'vacsbt86', 100301, 100006, '442279039053', 108, '暴走玩偶', '2025-09-11 12:43:44', 'create', '2025-09-11 20:43:44', '2025-09-11 20:43:44');
INSERT INTO `user_award_record_002` VALUES (90, 'jzzgm3_ntz', 100301, 100006, '597975380190', 101, '随机积分', '2025-09-11 12:43:46', 'completed', '2025-09-11 20:43:46', '2025-09-11 20:43:46');
INSERT INTO `user_award_record_002` VALUES (91, 'gztmft.i85', 100301, 100006, '476854803183', 107, '小霸王游戏机', '2025-09-11 12:43:48', 'create', '2025-09-11 20:43:47', '2025-09-11 20:43:47');
INSERT INTO `user_award_record_002` VALUES (92, 'izhlu8_kgs62', 100301, 100006, '699283069554', 104, '小米台灯', '2025-09-11 12:43:48', 'create', '2025-09-11 20:43:48', '2025-09-11 20:43:48');
INSERT INTO `user_award_record_002` VALUES (93, 'ttbtw9_hlm32', 100301, 100006, '561448789939', 108, '暴走玩偶', '2025-09-11 12:43:53', 'create', '2025-09-11 20:43:53', '2025-09-11 20:43:53');
INSERT INTO `user_award_record_002` VALUES (94, 'lm1_tti', 100301, 100006, '119920536560', 104, '小米台灯', '2025-09-11 12:43:55', 'create', '2025-09-11 20:43:54', '2025-09-11 20:43:54');

-- ----------------------------
-- Table structure for user_award_record_003
-- ----------------------------
DROP TABLE IF EXISTS `user_award_record_003`;
CREATE TABLE `user_award_record_003`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `strategy_id` bigint NOT NULL COMMENT '抽奖策略ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '抽奖订单ID【作为幂等使用】',
  `award_id` int NOT NULL COMMENT '奖品ID',
  `award_title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '奖品标题（名称）',
  `award_time` datetime NOT NULL COMMENT '中奖时间',
  `award_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'create' COMMENT '奖品状态；create-创建、completed-发奖完成',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_activity_id`(`activity_id` ASC) USING BTREE,
  INDEX `idx_award_id`(`strategy_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 257 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户中奖记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_award_record_003
-- ----------------------------
INSERT INTO `user_award_record_003` VALUES (2, 'Jay', 100301, 100006, '123251889509', 107, '小霸王游戏机', '2025-09-11 08:18:01', 'create', '2025-09-11 16:18:01', '2025-09-11 16:18:01');
INSERT INTO `user_award_record_003` VALUES (3, 'Jay', 100301, 100006, '047265279257', 104, '小米台灯', '2025-09-11 08:18:12', 'create', '2025-09-11 16:18:11', '2025-09-11 16:18:11');
INSERT INTO `user_award_record_003` VALUES (4, 'jay', 100301, 100006, '391754657215', 107, '小霸王游戏机', '2025-09-11 08:20:56', 'create', '2025-09-11 16:20:56', '2025-09-11 16:20:56');
INSERT INTO `user_award_record_003` VALUES (5, 'jay', 100301, 100006, '076933735647', 106, '轻奢办公椅', '2025-09-11 08:23:21', 'create', '2025-09-11 16:23:21', '2025-09-11 16:23:21');
INSERT INTO `user_award_record_003` VALUES (6, 'jay', 100301, 100006, '783289721604', 103, '支付优惠券', '2025-09-11 08:23:38', 'create', '2025-09-11 16:23:38', '2025-09-11 16:23:38');
INSERT INTO `user_award_record_003` VALUES (7, 'jay', 100301, 100006, '033552351982', 102, 'OpenAI会员卡', '2025-09-11 08:24:04', 'create', '2025-09-11 16:24:04', '2025-09-11 16:24:04');
INSERT INTO `user_award_record_003` VALUES (8, 'jay', 100301, 100006, '821015638921', 102, 'OpenAI会员卡', '2025-09-11 08:24:04', 'create', '2025-09-11 16:24:04', '2025-09-11 16:24:04');
INSERT INTO `user_award_record_003` VALUES (9, 'jay', 100301, 100006, '499159553296', 102, 'OpenAI会员卡', '2025-09-11 08:24:05', 'create', '2025-09-11 16:24:04', '2025-09-11 16:24:04');
INSERT INTO `user_award_record_003` VALUES (10, 'jay', 100301, 100006, '046267938929', 107, '小霸王游戏机', '2025-09-11 08:24:05', 'create', '2025-09-11 16:24:04', '2025-09-11 16:24:04');
INSERT INTO `user_award_record_003` VALUES (11, 'jay', 100301, 100006, '297927773450', 102, 'OpenAI会员卡', '2025-09-11 08:24:05', 'create', '2025-09-11 16:24:05', '2025-09-11 16:24:05');
INSERT INTO `user_award_record_003` VALUES (12, 'jay', 100301, 100006, '216637128225', 102, 'OpenAI会员卡', '2025-09-11 08:24:05', 'create', '2025-09-11 16:24:05', '2025-09-11 16:24:05');
INSERT INTO `user_award_record_003` VALUES (17, 'jay', 100301, 100006, '205945313683', 103, '支付优惠券', '2025-09-11 08:24:05', 'create', '2025-09-11 16:24:05', '2025-09-11 16:24:05');
INSERT INTO `user_award_record_003` VALUES (20, 'jay', 100301, 100006, '757591364360', 103, '支付优惠券', '2025-09-11 08:24:05', 'create', '2025-09-11 16:24:05', '2025-09-11 16:24:05');
INSERT INTO `user_award_record_003` VALUES (21, 'jay', 100301, 100006, '571613897942', 102, 'OpenAI会员卡', '2025-09-11 08:24:05', 'create', '2025-09-11 16:24:05', '2025-09-11 16:24:05');
INSERT INTO `user_award_record_003` VALUES (30, 'jay', 100301, 100006, '546832500103', 102, 'OpenAI会员卡', '2025-09-11 08:24:06', 'create', '2025-09-11 16:24:06', '2025-09-11 16:24:06');
INSERT INTO `user_award_record_003` VALUES (31, 'jay', 100301, 100006, '238802359406', 102, 'OpenAI会员卡', '2025-09-11 08:24:06', 'create', '2025-09-11 16:24:06', '2025-09-11 16:24:06');
INSERT INTO `user_award_record_003` VALUES (33, 'jay', 100301, 100006, '646916945448', 103, '支付优惠券', '2025-09-11 08:24:06', 'create', '2025-09-11 16:24:06', '2025-09-11 16:24:06');
INSERT INTO `user_award_record_003` VALUES (43, 'jay', 100301, 100006, '194147973545', 102, 'OpenAI会员卡', '2025-09-11 08:24:06', 'create', '2025-09-11 16:24:06', '2025-09-11 16:24:06');
INSERT INTO `user_award_record_003` VALUES (54, 'jay', 100301, 100006, '983748839600', 103, '支付优惠券', '2025-09-11 08:24:06', 'create', '2025-09-11 16:24:06', '2025-09-11 16:24:06');
INSERT INTO `user_award_record_003` VALUES (59, 'jay', 100301, 100006, '615094360243', 103, '支付优惠券', '2025-09-11 08:24:06', 'create', '2025-09-11 16:24:06', '2025-09-11 16:24:06');
INSERT INTO `user_award_record_003` VALUES (60, 'jay', 100301, 100006, '652310164400', 102, 'OpenAI会员卡', '2025-09-11 08:24:07', 'create', '2025-09-11 16:24:06', '2025-09-11 16:24:06');
INSERT INTO `user_award_record_003` VALUES (61, 'jay', 100301, 100006, '562470757638', 103, '支付优惠券', '2025-09-11 08:24:07', 'create', '2025-09-11 16:24:07', '2025-09-11 16:24:07');
INSERT INTO `user_award_record_003` VALUES (74, 'jay', 100301, 100006, '006241983283', 103, '支付优惠券', '2025-09-11 08:24:07', 'create', '2025-09-11 16:24:07', '2025-09-11 16:24:07');
INSERT INTO `user_award_record_003` VALUES (80, 'jay', 100301, 100006, '552393467348', 103, '支付优惠券', '2025-09-11 08:24:07', 'create', '2025-09-11 16:24:07', '2025-09-11 16:24:07');
INSERT INTO `user_award_record_003` VALUES (94, 'jay', 100301, 100006, '971520963966', 102, 'OpenAI会员卡', '2025-09-11 08:24:07', 'create', '2025-09-11 16:24:07', '2025-09-11 16:24:07');
INSERT INTO `user_award_record_003` VALUES (101, 'jay', 100301, 100006, '783707444418', 103, '支付优惠券', '2025-09-11 08:24:08', 'create', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `user_award_record_003` VALUES (103, 'jay', 100301, 100006, '441429068493', 103, '支付优惠券', '2025-09-11 08:24:08', 'create', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `user_award_record_003` VALUES (104, 'jay', 100301, 100006, '237545059288', 103, '支付优惠券', '2025-09-11 08:24:08', 'create', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `user_award_record_003` VALUES (105, 'jay', 100301, 100006, '390428969669', 102, 'OpenAI会员卡', '2025-09-11 08:24:08', 'create', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `user_award_record_003` VALUES (110, 'jay', 100301, 100006, '523135197581', 102, 'OpenAI会员卡', '2025-09-11 08:24:08', 'create', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `user_award_record_003` VALUES (112, 'jay', 100301, 100006, '443766981895', 102, 'OpenAI会员卡', '2025-09-11 08:24:08', 'create', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `user_award_record_003` VALUES (118, 'jay', 100301, 100006, '961659922796', 103, '支付优惠券', '2025-09-11 08:24:09', 'create', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `user_award_record_003` VALUES (121, 'jay', 100301, 100006, '091048707105', 103, '支付优惠券', '2025-09-11 08:24:09', 'create', '2025-09-11 16:24:09', '2025-09-11 16:24:09');
INSERT INTO `user_award_record_003` VALUES (122, 'jay', 100301, 100006, '669079823424', 103, '支付优惠券', '2025-09-11 08:24:09', 'create', '2025-09-11 16:24:09', '2025-09-11 16:24:09');
INSERT INTO `user_award_record_003` VALUES (123, 'jay', 100301, 100006, '659624201810', 102, 'OpenAI会员卡', '2025-09-11 08:24:09', 'create', '2025-09-11 16:24:09', '2025-09-11 16:24:09');
INSERT INTO `user_award_record_003` VALUES (124, 'jay', 100301, 100006, '283040982965', 103, '支付优惠券', '2025-09-11 08:24:09', 'create', '2025-09-11 16:24:09', '2025-09-11 16:24:09');
INSERT INTO `user_award_record_003` VALUES (125, 'jay', 100301, 100006, '812669656316', 102, 'OpenAI会员卡', '2025-09-11 08:24:09', 'create', '2025-09-11 16:24:09', '2025-09-11 16:24:09');
INSERT INTO `user_award_record_003` VALUES (126, 'jay', 100301, 100006, '063456093801', 102, 'OpenAI会员卡', '2025-09-11 08:24:10', 'create', '2025-09-11 16:24:09', '2025-09-11 16:24:09');
INSERT INTO `user_award_record_003` VALUES (127, 'jay', 100301, 100006, '307575448159', 103, '支付优惠券', '2025-09-11 08:24:10', 'create', '2025-09-11 16:24:10', '2025-09-11 16:24:10');
INSERT INTO `user_award_record_003` VALUES (128, 'jay', 100301, 100006, '872514825381', 103, '支付优惠券', '2025-09-11 08:24:10', 'create', '2025-09-11 16:24:10', '2025-09-11 16:24:10');
INSERT INTO `user_award_record_003` VALUES (129, 'jay', 100301, 100006, '867998269250', 103, '支付优惠券', '2025-09-11 08:24:10', 'create', '2025-09-11 16:24:10', '2025-09-11 16:24:10');
INSERT INTO `user_award_record_003` VALUES (130, 'jay', 100301, 100006, '455875088253', 103, '支付优惠券', '2025-09-11 08:24:10', 'create', '2025-09-11 16:24:10', '2025-09-11 16:24:10');
INSERT INTO `user_award_record_003` VALUES (131, 'jay', 100301, 100006, '431923721932', 103, '支付优惠券', '2025-09-11 08:24:10', 'create', '2025-09-11 16:24:10', '2025-09-11 16:24:10');
INSERT INTO `user_award_record_003` VALUES (132, 'jay', 100301, 100006, '526176379196', 102, 'OpenAI会员卡', '2025-09-11 08:24:10', 'create', '2025-09-11 16:24:10', '2025-09-11 16:24:10');
INSERT INTO `user_award_record_003` VALUES (133, 'jay', 100301, 100006, '539334579100', 102, 'OpenAI会员卡', '2025-09-11 08:25:48', 'create', '2025-09-11 16:25:47', '2025-09-11 16:25:47');
INSERT INTO `user_award_record_003` VALUES (134, 'jay', 100301, 100006, '133844269407', 102, 'OpenAI会员卡', '2025-09-11 08:25:48', 'create', '2025-09-11 16:25:47', '2025-09-11 16:25:47');
INSERT INTO `user_award_record_003` VALUES (135, 'jay', 100301, 100006, '114307413504', 102, 'OpenAI会员卡', '2025-09-11 08:25:48', 'create', '2025-09-11 16:25:48', '2025-09-11 16:25:48');
INSERT INTO `user_award_record_003` VALUES (136, 'jay', 100301, 100006, '793842208878', 102, 'OpenAI会员卡', '2025-09-11 08:25:48', 'create', '2025-09-11 16:25:48', '2025-09-11 16:25:48');
INSERT INTO `user_award_record_003` VALUES (137, 'jay', 100301, 100006, '071707115347', 103, '支付优惠券', '2025-09-11 08:25:48', 'create', '2025-09-11 16:25:48', '2025-09-11 16:25:48');
INSERT INTO `user_award_record_003` VALUES (138, 'jay', 100301, 100006, '160343840537', 102, 'OpenAI会员卡', '2025-09-11 08:25:48', 'create', '2025-09-11 16:25:48', '2025-09-11 16:25:48');
INSERT INTO `user_award_record_003` VALUES (139, 'jay', 100301, 100006, '781957534904', 103, '支付优惠券', '2025-09-11 08:25:48', 'create', '2025-09-11 16:25:48', '2025-09-11 16:25:48');
INSERT INTO `user_award_record_003` VALUES (140, 'jay', 100301, 100006, '847007343147', 103, '支付优惠券', '2025-09-11 08:25:49', 'create', '2025-09-11 16:25:48', '2025-09-11 16:25:48');
INSERT INTO `user_award_record_003` VALUES (141, 'jay', 100301, 100006, '333682254849', 103, '支付优惠券', '2025-09-11 08:25:49', 'create', '2025-09-11 16:25:49', '2025-09-11 16:25:49');
INSERT INTO `user_award_record_003` VALUES (142, 'jay', 100301, 100006, '558331798435', 102, 'OpenAI会员卡', '2025-09-11 08:25:49', 'create', '2025-09-11 16:25:49', '2025-09-11 16:25:49');
INSERT INTO `user_award_record_003` VALUES (143, 'jay', 100301, 100006, '026648834930', 103, '支付优惠券', '2025-09-11 08:25:49', 'create', '2025-09-11 16:25:49', '2025-09-11 16:25:49');
INSERT INTO `user_award_record_003` VALUES (144, 'jay', 100301, 100006, '743509727445', 103, '支付优惠券', '2025-09-11 08:25:49', 'create', '2025-09-11 16:25:49', '2025-09-11 16:25:49');
INSERT INTO `user_award_record_003` VALUES (145, 'jay', 100301, 100006, '074378267714', 103, '支付优惠券', '2025-09-11 08:25:49', 'create', '2025-09-11 16:25:49', '2025-09-11 16:25:49');
INSERT INTO `user_award_record_003` VALUES (146, 'jay', 100301, 100006, '508985175657', 102, 'OpenAI会员卡', '2025-09-11 08:25:50', 'create', '2025-09-11 16:25:49', '2025-09-11 16:25:49');
INSERT INTO `user_award_record_003` VALUES (147, 'jay', 100301, 100006, '549839403269', 102, 'OpenAI会员卡', '2025-09-11 08:25:50', 'create', '2025-09-11 16:25:50', '2025-09-11 16:25:50');
INSERT INTO `user_award_record_003` VALUES (148, 'jay', 100301, 100006, '070452517982', 103, '支付优惠券', '2025-09-11 08:25:50', 'create', '2025-09-11 16:25:50', '2025-09-11 16:25:50');
INSERT INTO `user_award_record_003` VALUES (149, 'jay', 100301, 100006, '932629270157', 103, '支付优惠券', '2025-09-11 08:25:50', 'create', '2025-09-11 16:25:50', '2025-09-11 16:25:50');
INSERT INTO `user_award_record_003` VALUES (150, 'jay', 100301, 100006, '831208651493', 102, 'OpenAI会员卡', '2025-09-11 08:25:50', 'create', '2025-09-11 16:25:50', '2025-09-11 16:25:50');
INSERT INTO `user_award_record_003` VALUES (151, 'jay', 100301, 100006, '755053385182', 103, '支付优惠券', '2025-09-11 08:25:50', 'create', '2025-09-11 16:25:50', '2025-09-11 16:25:50');
INSERT INTO `user_award_record_003` VALUES (152, 'jay', 100301, 100006, '516887892627', 102, 'OpenAI会员卡', '2025-09-11 08:25:51', 'create', '2025-09-11 16:25:50', '2025-09-11 16:25:50');
INSERT INTO `user_award_record_003` VALUES (153, 'jay', 100301, 100006, '479504085878', 103, '支付优惠券', '2025-09-11 08:25:51', 'create', '2025-09-11 16:25:51', '2025-09-11 16:25:51');
INSERT INTO `user_award_record_003` VALUES (154, 'jay', 100301, 100006, '768119144839', 102, 'OpenAI会员卡', '2025-09-11 08:25:51', 'create', '2025-09-11 16:25:51', '2025-09-11 16:25:51');
INSERT INTO `user_award_record_003` VALUES (155, 'jay', 100301, 100006, '144414259715', 102, 'OpenAI会员卡', '2025-09-11 08:25:51', 'create', '2025-09-11 16:25:51', '2025-09-11 16:25:51');
INSERT INTO `user_award_record_003` VALUES (156, 'jay', 100301, 100006, '773169473027', 103, '支付优惠券', '2025-09-11 08:25:51', 'create', '2025-09-11 16:25:51', '2025-09-11 16:25:51');
INSERT INTO `user_award_record_003` VALUES (157, 'jay', 100301, 100006, '065288892123', 107, '小霸王游戏机', '2025-09-11 08:25:52', 'create', '2025-09-11 16:25:51', '2025-09-11 16:25:51');
INSERT INTO `user_award_record_003` VALUES (158, 'jay', 100301, 100006, '985109117801', 107, '小霸王游戏机', '2025-09-11 08:25:52', 'create', '2025-09-11 16:25:52', '2025-09-11 16:25:52');
INSERT INTO `user_award_record_003` VALUES (159, 'jay', 100301, 100006, '783215776478', 106, '轻奢办公椅', '2025-09-11 08:25:52', 'create', '2025-09-11 16:25:52', '2025-09-11 16:25:52');
INSERT INTO `user_award_record_003` VALUES (160, 'jay', 100301, 100006, '839870176735', 107, '小霸王游戏机', '2025-09-11 08:25:52', 'create', '2025-09-11 16:25:52', '2025-09-11 16:25:52');
INSERT INTO `user_award_record_003` VALUES (161, 'jay', 100301, 100006, '664037492884', 107, '小霸王游戏机', '2025-09-11 08:25:52', 'create', '2025-09-11 16:25:52', '2025-09-11 16:25:52');
INSERT INTO `user_award_record_003` VALUES (162, 'jay', 100301, 100006, '586084309178', 107, '小霸王游戏机', '2025-09-11 08:25:52', 'create', '2025-09-11 16:25:52', '2025-09-11 16:25:52');
INSERT INTO `user_award_record_003` VALUES (163, 'jay', 100301, 100006, '956670727490', 106, '轻奢办公椅', '2025-09-11 08:25:52', 'create', '2025-09-11 16:25:52', '2025-09-11 16:25:52');
INSERT INTO `user_award_record_003` VALUES (164, 'jay', 100301, 100006, '031398518710', 107, '小霸王游戏机', '2025-09-11 08:25:53', 'create', '2025-09-11 16:25:52', '2025-09-11 16:25:52');
INSERT INTO `user_award_record_003` VALUES (165, 'jay', 100301, 100006, '915593783833', 106, '轻奢办公椅', '2025-09-11 08:25:53', 'create', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `user_award_record_003` VALUES (166, 'jay', 100301, 100006, '102342243297', 106, '轻奢办公椅', '2025-09-11 08:25:53', 'create', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `user_award_record_003` VALUES (167, 'jay', 100301, 100006, '174948679152', 106, '轻奢办公椅', '2025-09-11 08:25:53', 'create', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `user_award_record_003` VALUES (168, 'jay', 100301, 100006, '455316181452', 107, '小霸王游戏机', '2025-09-11 08:25:53', 'create', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `user_award_record_003` VALUES (169, 'jay', 100301, 100006, '165163642098', 107, '小霸王游戏机', '2025-09-11 08:25:53', 'create', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `user_award_record_003` VALUES (170, 'jay', 100301, 100006, '258394248391', 107, '小霸王游戏机', '2025-09-11 08:25:53', 'create', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `user_award_record_003` VALUES (171, 'jay', 100301, 100006, '514988530785', 106, '轻奢办公椅', '2025-09-11 08:25:54', 'create', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `user_award_record_003` VALUES (172, 'jay', 100301, 100006, '460369076599', 107, '小霸王游戏机', '2025-09-11 08:25:54', 'create', '2025-09-11 16:25:54', '2025-09-11 16:25:54');
INSERT INTO `user_award_record_003` VALUES (173, 'jay', 100301, 100006, '723454370226', 106, '轻奢办公椅', '2025-09-11 08:25:54', 'create', '2025-09-11 16:25:54', '2025-09-11 16:25:54');
INSERT INTO `user_award_record_003` VALUES (174, 'jay', 100301, 100006, '902258464786', 106, '轻奢办公椅', '2025-09-11 08:25:54', 'create', '2025-09-11 16:25:54', '2025-09-11 16:25:54');
INSERT INTO `user_award_record_003` VALUES (175, 'jay', 100301, 100006, '224233858952', 107, '小霸王游戏机', '2025-09-11 08:25:54', 'create', '2025-09-11 16:25:54', '2025-09-11 16:25:54');
INSERT INTO `user_award_record_003` VALUES (176, 'jay', 100301, 100006, '960866059786', 106, '轻奢办公椅', '2025-09-11 08:25:54', 'create', '2025-09-11 16:25:54', '2025-09-11 16:25:54');
INSERT INTO `user_award_record_003` VALUES (177, 'jay', 100301, 100006, '361355765275', 106, '轻奢办公椅', '2025-09-11 08:25:54', 'create', '2025-09-11 16:25:54', '2025-09-11 16:25:54');
INSERT INTO `user_award_record_003` VALUES (178, 'jay', 100301, 100006, '885952284004', 106, '轻奢办公椅', '2025-09-11 08:25:55', 'create', '2025-09-11 16:25:55', '2025-09-11 16:25:55');
INSERT INTO `user_award_record_003` VALUES (179, 'jay', 100301, 100006, '083192581036', 107, '小霸王游戏机', '2025-09-11 08:25:55', 'create', '2025-09-11 16:25:55', '2025-09-11 16:25:55');
INSERT INTO `user_award_record_003` VALUES (180, 'jay', 100301, 100006, '971260585952', 106, '轻奢办公椅', '2025-09-11 11:57:59', 'create', '2025-09-11 19:57:59', '2025-09-11 19:57:59');
INSERT INTO `user_award_record_003` VALUES (181, 'jay', 100301, 100006, '349104647196', 106, '轻奢办公椅', '2025-09-11 12:16:06', 'create', '2025-09-11 20:16:06', '2025-09-11 20:16:06');
INSERT INTO `user_award_record_003` VALUES (182, 'jay', 100301, 100006, '520284234489', 106, '轻奢办公椅', '2025-09-11 12:35:56', 'create', '2025-09-11 20:35:55', '2025-09-11 20:35:55');
INSERT INTO `user_award_record_003` VALUES (183, 'jay', 100301, 100006, '584585502041', 106, '轻奢办公椅', '2025-09-11 12:37:04', 'create', '2025-09-11 20:37:04', '2025-09-11 20:37:04');
INSERT INTO `user_award_record_003` VALUES (184, 'jay', 100301, 100006, '325253046571', 106, '轻奢办公椅', '2025-09-11 12:37:04', 'create', '2025-09-11 20:37:04', '2025-09-11 20:37:04');
INSERT INTO `user_award_record_003` VALUES (185, 'jay', 100301, 100006, '516850082813', 106, '轻奢办公椅', '2025-09-11 12:37:04', 'create', '2025-09-11 20:37:04', '2025-09-11 20:37:04');
INSERT INTO `user_award_record_003` VALUES (186, 'jay', 100301, 100006, '034783208182', 107, '小霸王游戏机', '2025-09-11 12:37:05', 'create', '2025-09-11 20:37:05', '2025-09-11 20:37:05');
INSERT INTO `user_award_record_003` VALUES (187, 'jay', 100301, 100006, '619643666484', 106, '轻奢办公椅', '2025-09-11 12:37:05', 'create', '2025-09-11 20:37:05', '2025-09-11 20:37:05');
INSERT INTO `user_award_record_003` VALUES (188, 'jay', 100301, 100006, '023739472554', 106, '轻奢办公椅', '2025-09-11 12:37:05', 'create', '2025-09-11 20:37:05', '2025-09-11 20:37:05');
INSERT INTO `user_award_record_003` VALUES (189, 'jay', 100301, 100006, '469960539768', 107, '小霸王游戏机', '2025-09-11 12:37:06', 'create', '2025-09-11 20:37:05', '2025-09-11 20:37:05');
INSERT INTO `user_award_record_003` VALUES (190, 'jay', 100301, 100006, '721474107557', 106, '轻奢办公椅', '2025-09-11 12:37:06', 'create', '2025-09-11 20:37:05', '2025-09-11 20:37:05');
INSERT INTO `user_award_record_003` VALUES (191, 'jay', 100301, 100006, '510549235023', 106, '轻奢办公椅', '2025-09-11 12:37:06', 'create', '2025-09-11 20:37:06', '2025-09-11 20:37:06');
INSERT INTO `user_award_record_003` VALUES (192, 'jay', 100301, 100006, '693555010533', 106, '轻奢办公椅', '2025-09-11 12:37:06', 'create', '2025-09-11 20:37:06', '2025-09-11 20:37:06');
INSERT INTO `user_award_record_003` VALUES (193, 'l7nrqi39', 100301, 100006, '001082865194', 101, '随机积分', '2025-09-11 12:37:07', 'completed', '2025-09-11 20:37:07', '2025-09-11 20:37:07');
INSERT INTO `user_award_record_003` VALUES (196, 'jay', 100301, 100006, '870993828346', 106, '轻奢办公椅', '2025-09-11 12:40:05', 'create', '2025-09-11 20:40:05', '2025-09-11 20:40:05');
INSERT INTO `user_award_record_003` VALUES (197, 'jay', 100301, 100006, '298068288177', 107, '小霸王游戏机', '2025-09-11 12:40:32', 'create', '2025-09-11 20:40:32', '2025-09-11 20:40:32');
INSERT INTO `user_award_record_003` VALUES (198, 'l7nrqi39', 100301, 100006, '703591723821', 101, '随机积分', '2025-09-11 12:40:35', 'completed', '2025-09-11 20:40:35', '2025-09-11 20:40:35');
INSERT INTO `user_award_record_003` VALUES (199, 'lh7lnf.iul17', 100301, 100006, '756798941877', 103, '支付优惠券', '2025-09-11 12:40:37', 'create', '2025-09-11 20:40:37', '2025-09-11 20:40:37');
INSERT INTO `user_award_record_003` VALUES (200, 'ggqryd_skd44', 100301, 100006, '202461115156', 101, '随机积分', '2025-09-11 12:40:42', 'completed', '2025-09-11 20:40:42', '2025-09-11 20:40:42');
INSERT INTO `user_award_record_003` VALUES (201, 'lm1_p1k', 100301, 100006, '223908492570', 101, '随机积分', '2025-09-11 12:40:46', 'completed', '2025-09-11 20:40:45', '2025-09-11 20:40:45');
INSERT INTO `user_award_record_003` VALUES (202, 'pgknz9.q5795', 100301, 100006, '084473582561', 103, '支付优惠券', '2025-09-11 12:40:48', 'create', '2025-09-11 20:40:47', '2025-09-11 20:40:47');
INSERT INTO `user_award_record_003` VALUES (203, 'gs2m5650', 100301, 100006, '819518130540', 101, '随机积分', '2025-09-11 12:40:48', 'completed', '2025-09-11 20:40:48', '2025-09-11 20:40:48');
INSERT INTO `user_award_record_003` VALUES (204, 'lm1i2v83', 100301, 100006, '618271231279', 108, '暴走玩偶', '2025-09-11 12:40:54', 'create', '2025-09-11 20:40:53', '2025-09-11 20:40:53');
INSERT INTO `user_award_record_003` VALUES (205, 'tt6kfy43', 100301, 100006, '617818152639', 102, 'OpenAI会员卡', '2025-09-11 12:40:56', 'create', '2025-09-11 20:40:56', '2025-09-11 20:40:56');
INSERT INTO `user_award_record_003` VALUES (206, 'i2xmy2_fj6', 100301, 100006, '729341864240', 103, '支付优惠券', '2025-09-11 12:41:03', 'create', '2025-09-11 20:41:02', '2025-09-11 20:41:02');
INSERT INTO `user_award_record_003` VALUES (207, 'izhpd6_jwt19', 100301, 100006, '514193364784', 107, '小霸王游戏机', '2025-09-11 12:41:03', 'create', '2025-09-11 20:41:03', '2025-09-11 20:41:03');
INSERT INTO `user_award_record_003` VALUES (208, 'mawfmn82', 100301, 100006, '892744627134', 101, '随机积分', '2025-09-11 12:41:07', 'completed', '2025-09-11 20:41:06', '2025-09-11 20:41:06');
INSERT INTO `user_award_record_003` VALUES (209, 'fmnrmi_ff1', 100301, 100006, '881604419574', 107, '小霸王游戏机', '2025-09-11 12:41:07', 'create', '2025-09-11 20:41:07', '2025-09-11 20:41:07');
INSERT INTO `user_award_record_003` VALUES (210, 'v9rlnf.gbl49', 100301, 100006, '695698496764', 102, 'OpenAI会员卡', '2025-09-11 12:41:09', 'create', '2025-09-11 20:41:09', '2025-09-11 20:41:09');
INSERT INTO `user_award_record_003` VALUES (211, 'v9ru724', 100301, 100006, '789400922936', 101, '随机积分', '2025-09-11 12:41:14', 'completed', '2025-09-11 20:41:14', '2025-09-11 20:41:14');
INSERT INTO `user_award_record_003` VALUES (212, 'izhlq4.lpvfim58', 100301, 100006, '907577096403', 108, '暴走玩偶', '2025-09-11 12:41:19', 'create', '2025-09-11 20:41:19', '2025-09-11 20:41:19');
INSERT INTO `user_award_record_003` VALUES (213, 'mawtue37', 100301, 100006, '892261763789', 108, '暴走玩偶', '2025-09-11 12:41:22', 'create', '2025-09-11 20:41:22', '2025-09-11 20:41:22');
INSERT INTO `user_award_record_003` VALUES (214, 'jwupu524', 100301, 100006, '249155116559', 107, '小霸王游戏机', '2025-09-11 12:41:23', 'create', '2025-09-11 20:41:23', '2025-09-11 20:41:23');
INSERT INTO `user_award_record_003` VALUES (215, 'tbfnuy58', 100301, 100006, '488087498598', 101, '随机积分', '2025-09-11 12:41:28', 'completed', '2025-09-11 20:41:27', '2025-09-11 20:41:28');
INSERT INTO `user_award_record_003` VALUES (216, 'ffxlmf_l9v84', 100301, 100006, '882275162643', 101, '随机积分', '2025-09-11 12:41:29', 'completed', '2025-09-11 20:41:28', '2025-09-11 20:41:28');
INSERT INTO `user_award_record_003` VALUES (217, 'irescp12', 100301, 100006, '495205823805', 104, '小米台灯', '2025-09-11 12:41:31', 'create', '2025-09-11 20:41:31', '2025-09-11 20:41:31');
INSERT INTO `user_award_record_003` VALUES (218, 'mawfmn_rlk', 100301, 100006, '633936992398', 107, '小霸王游戏机', '2025-09-11 12:41:37', 'create', '2025-09-11 20:41:36', '2025-09-11 20:41:36');
INSERT INTO `user_award_record_003` VALUES (219, 'izhpd6_lxi', 100301, 100006, '830180589722', 104, '小米台灯', '2025-09-11 12:41:39', 'create', '2025-09-11 20:41:39', '2025-09-11 20:41:39');
INSERT INTO `user_award_record_003` VALUES (220, 'v9rmdm_gp82', 100301, 100006, '160882356236', 101, '随机积分', '2025-09-11 12:41:44', 'completed', '2025-09-11 20:41:43', '2025-09-11 20:41:43');
INSERT INTO `user_award_record_003` VALUES (221, 'kutqrx_lal54', 100301, 100006, '478453058390', 107, '小霸王游戏机', '2025-09-11 12:41:49', 'create', '2025-09-11 20:41:49', '2025-09-11 20:41:49');
INSERT INTO `user_award_record_003` VALUES (222, 'rmftt4.k28', 100301, 100006, '536016673960', 101, '随机积分', '2025-09-11 12:41:51', 'completed', '2025-09-11 20:41:51', '2025-09-11 20:41:51');
INSERT INTO `user_award_record_003` VALUES (223, 'k8qscp97', 100301, 100006, '705400855619', 101, '随机积分', '2025-09-11 12:41:53', 'completed', '2025-09-11 20:41:52', '2025-09-11 20:41:52');
INSERT INTO `user_award_record_003` VALUES (224, 'ixjlj1_kk17', 100301, 100006, '301912636493', 107, '小霸王游戏机', '2025-09-11 12:41:53', 'create', '2025-09-11 20:41:53', '2025-09-11 20:41:53');
INSERT INTO `user_award_record_003` VALUES (225, 'p72k3d.kjv', 100301, 100006, '048535936908', 104, '小米台灯', '2025-09-11 12:41:56', 'create', '2025-09-11 20:41:55', '2025-09-11 20:41:55');
INSERT INTO `user_award_record_003` VALUES (226, 'ttbtw9_lr4', 100301, 100006, '102266291534', 107, '小霸王游戏机', '2025-09-11 12:42:06', 'create', '2025-09-11 20:42:06', '2025-09-11 20:42:06');
INSERT INTO `user_award_record_003` VALUES (227, 'ffxlmf_fk177', 100301, 100006, '702310873139', 107, '小霸王游戏机', '2025-09-11 12:42:11', 'create', '2025-09-11 20:42:11', '2025-09-11 20:42:11');
INSERT INTO `user_award_record_003` VALUES (228, 'v9rlnf_iwn40', 100301, 100006, '946574004672', 101, '随机积分', '2025-09-11 12:42:16', 'completed', '2025-09-11 20:42:16', '2025-09-11 20:42:16');
INSERT INTO `user_award_record_003` VALUES (229, 'k5qkfk_tj383', 100301, 100006, '001363804835', 104, '小米台灯', '2025-09-11 12:42:17', 'create', '2025-09-11 20:42:16', '2025-09-11 20:42:16');
INSERT INTO `user_award_record_003` VALUES (230, 'jcvsbt_rx7', 100301, 100006, '611789078193', 103, '支付优惠券', '2025-09-11 12:42:29', 'create', '2025-09-11 20:42:29', '2025-09-11 20:42:29');
INSERT INTO `user_award_record_003` VALUES (231, 'ui839', 100301, 100006, '198687250750', 101, '随机积分', '2025-09-11 12:42:33', 'completed', '2025-09-11 20:42:32', '2025-09-11 20:42:32');
INSERT INTO `user_award_record_003` VALUES (232, 'k5qscp59', 100301, 100006, '133167573381', 107, '小霸王游戏机', '2025-09-11 12:42:35', 'create', '2025-09-11 20:42:35', '2025-09-11 20:42:35');
INSERT INTO `user_award_record_003` VALUES (233, 'ppm.nz1', 100301, 100006, '398530093132', 101, '随机积分', '2025-09-11 12:42:39', 'completed', '2025-09-11 20:42:39', '2025-09-11 20:42:39');
INSERT INTO `user_award_record_003` VALUES (234, 'i1cvdk65', 100301, 100006, '837323220757', 108, '暴走玩偶', '2025-09-11 12:42:40', 'create', '2025-09-11 20:42:39', '2025-09-11 20:42:39');
INSERT INTO `user_award_record_003` VALUES (235, 'fmnnzu.keb98', 100301, 100006, '345776518808', 107, '小霸王游戏机', '2025-09-11 12:42:46', 'create', '2025-09-11 20:42:46', '2025-09-11 20:42:46');
INSERT INTO `user_award_record_003` VALUES (236, 'negtue_psi98', 100301, 100006, '089682630429', 104, '小米台灯', '2025-09-11 12:42:57', 'create', '2025-09-11 20:42:56', '2025-09-11 20:42:56');
INSERT INTO `user_award_record_003` VALUES (237, 'mawnuy_hce', 100301, 100006, '493552799216', 103, '支付优惠券', '2025-09-11 12:43:02', 'create', '2025-09-11 20:43:01', '2025-09-11 20:43:01');
INSERT INTO `user_award_record_003` VALUES (238, 'lmfmhh_kd7', 100301, 100006, '868485637198', 107, '小霸王游戏机', '2025-09-11 12:43:10', 'create', '2025-09-11 20:43:09', '2025-09-11 20:43:09');
INSERT INTO `user_award_record_003` VALUES (239, 'ggqk1z.fj980', 100301, 100006, '581537762946', 108, '暴走玩偶', '2025-09-11 12:43:16', 'create', '2025-09-11 20:43:16', '2025-09-11 20:43:16');
INSERT INTO `user_award_record_003` VALUES (240, 'ryilj19', 100301, 100006, '761018521456', 104, '小米台灯', '2025-09-11 12:43:17', 'create', '2025-09-11 20:43:17', '2025-09-11 20:43:17');
INSERT INTO `user_award_record_003` VALUES (241, 'ggqk1z31', 100301, 100006, '273433046821', 101, '随机积分', '2025-09-11 12:43:20', 'completed', '2025-09-11 20:43:20', '2025-09-11 20:43:20');
INSERT INTO `user_award_record_003` VALUES (242, 'lh7lnf.g53', 100301, 100006, '218012945362', 104, '小米台灯', '2025-09-11 12:43:22', 'create', '2025-09-11 20:43:22', '2025-09-11 20:43:22');
INSERT INTO `user_award_record_003` VALUES (243, 'mawfmn57', 100301, 100006, '453407879495', 102, 'OpenAI会员卡', '2025-09-11 12:43:24', 'create', '2025-09-11 20:43:23', '2025-09-11 20:43:23');
INSERT INTO `user_award_record_003` VALUES (244, 'i8lv9r.jdb', 100301, 100006, '792496894843', 108, '暴走玩偶', '2025-09-11 12:43:25', 'create', '2025-09-11 20:43:25', '2025-09-11 20:43:25');
INSERT INTO `user_award_record_003` VALUES (245, 'ts5hvb.q7z14', 100301, 100006, '846782563149', 103, '支付优惠券', '2025-09-11 12:43:27', 'create', '2025-09-11 20:43:27', '2025-09-11 20:43:27');
INSERT INTO `user_award_record_003` VALUES (246, 'jcvsbt87', 100301, 100006, '294551180693', 104, '小米台灯', '2025-09-11 12:43:30', 'create', '2025-09-11 20:43:30', '2025-09-11 20:43:30');
INSERT INTO `user_award_record_003` VALUES (247, 'k5mmcx74', 100301, 100006, '735137035944', 108, '暴走玩偶', '2025-09-11 12:43:32', 'create', '2025-09-11 20:43:31', '2025-09-11 20:43:31');
INSERT INTO `user_award_record_003` VALUES (248, 'fvpts4_qg435', 100301, 100006, '174887400116', 107, '小霸王游戏机', '2025-09-11 12:43:36', 'create', '2025-09-11 20:43:35', '2025-09-11 20:43:35');
INSERT INTO `user_award_record_003` VALUES (249, 'i1clo5_oti', 100301, 100006, '336707501296', 101, '随机积分', '2025-09-11 12:43:37', 'completed', '2025-09-11 20:43:37', '2025-09-11 20:43:37');
INSERT INTO `user_award_record_003` VALUES (250, 'fmnnzu.sib', 100301, 100006, '598280794989', 101, '随机积分', '2025-09-11 12:43:39', 'completed', '2025-09-11 20:43:38', '2025-09-11 20:43:39');
INSERT INTO `user_award_record_003` VALUES (251, 'hm1nuy_lid', 100301, 100006, '394242850896', 104, '小米台灯', '2025-09-11 12:43:40', 'create', '2025-09-11 20:43:40', '2025-09-11 20:43:40');
INSERT INTO `user_award_record_003` VALUES (252, 'jlbffg18', 100301, 100006, '030092172986', 102, 'OpenAI会员卡', '2025-09-11 12:43:41', 'create', '2025-09-11 20:43:41', '2025-09-11 20:43:41');
INSERT INTO `user_award_record_003` VALUES (253, 'pgknz911', 100301, 100006, '368273962486', 107, '小霸王游戏机', '2025-09-11 12:43:42', 'create', '2025-09-11 20:43:42', '2025-09-11 20:43:42');
INSERT INTO `user_award_record_003` VALUES (254, 'k8qlqi.nel95', 100301, 100006, '126181316058', 108, '暴走玩偶', '2025-09-11 12:43:47', 'create', '2025-09-11 20:43:46', '2025-09-11 20:43:46');
INSERT INTO `user_award_record_003` VALUES (255, 'fgwuih.gkv94', 100301, 100006, '296798147319', 102, 'OpenAI会员卡', '2025-09-11 12:43:52', 'create', '2025-09-11 20:43:52', '2025-09-11 20:43:52');
INSERT INTO `user_award_record_003` VALUES (256, 'jay', 100301, 100006, '425692335025', 107, '小霸王游戏机', '2025-09-12 04:14:34', 'create', '2025-09-12 12:14:34', '2025-09-12 12:14:34');

-- ----------------------------
-- Table structure for user_behavior_rebate_order_000
-- ----------------------------
DROP TABLE IF EXISTS `user_behavior_rebate_order_000`;
CREATE TABLE `user_behavior_rebate_order_000`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单ID',
  `behavior_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '行为类型（sign 签到、openai_pay 支付）',
  `rebate_desc` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '返利描述',
  `rebate_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '返利类型（sku 活动库存充值商品、integral 用户活动积分）',
  `rebate_config` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '返利配置【sku值，积分值】',
  `out_business_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务仿重ID - 外部透传，方便查询使用',
  `biz_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务ID - 拼接的唯一值。拼接 out_business_no + 自身枚举',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  UNIQUE INDEX `uq_biz_id`(`biz_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户行为返利流水订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_behavior_rebate_order_000
-- ----------------------------

-- ----------------------------
-- Table structure for user_behavior_rebate_order_001
-- ----------------------------
DROP TABLE IF EXISTS `user_behavior_rebate_order_001`;
CREATE TABLE `user_behavior_rebate_order_001`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单ID',
  `behavior_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '行为类型（sign 签到、openai_pay 支付）',
  `rebate_desc` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '返利描述',
  `rebate_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '返利类型（sku 活动库存充值商品、integral 用户活动积分）',
  `rebate_config` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '返利配置【sku值，积分值】',
  `out_business_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务仿重ID - 外部透传，方便查询使用',
  `biz_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务ID - 拼接的唯一值。拼接 out_business_no + 自身枚举',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  UNIQUE INDEX `uq_biz_id`(`biz_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 44 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户行为返利流水订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_behavior_rebate_order_001
-- ----------------------------

-- ----------------------------
-- Table structure for user_behavior_rebate_order_002
-- ----------------------------
DROP TABLE IF EXISTS `user_behavior_rebate_order_002`;
CREATE TABLE `user_behavior_rebate_order_002`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单ID',
  `behavior_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '行为类型（sign 签到、openai_pay 支付）',
  `rebate_desc` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '返利描述',
  `rebate_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '返利类型（sku 活动库存充值商品、integral 用户活动积分）',
  `rebate_config` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '返利配置【sku值，积分值】',
  `out_business_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务仿重ID - 外部透传，方便查询使用',
  `biz_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务ID - 拼接的唯一值。拼接 out_business_no + 自身枚举',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  UNIQUE INDEX `uq_biz_id`(`biz_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户行为返利流水订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_behavior_rebate_order_002
-- ----------------------------

-- ----------------------------
-- Table structure for user_behavior_rebate_order_003
-- ----------------------------
DROP TABLE IF EXISTS `user_behavior_rebate_order_003`;
CREATE TABLE `user_behavior_rebate_order_003`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单ID',
  `behavior_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '行为类型（sign 签到、openai_pay 支付）',
  `rebate_desc` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '返利描述',
  `rebate_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '返利类型（sku 活动库存充值商品、integral 用户活动积分）',
  `rebate_config` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '返利配置【sku值，积分值】',
  `out_business_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务仿重ID - 外部透传，方便查询使用',
  `biz_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务ID - 拼接的唯一值。拼接 out_business_no + 自身枚举',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  UNIQUE INDEX `uq_biz_id`(`biz_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户行为返利流水订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_behavior_rebate_order_003
-- ----------------------------

-- ----------------------------
-- Table structure for user_credit_account
-- ----------------------------
DROP TABLE IF EXISTS `user_credit_account`;
CREATE TABLE `user_credit_account`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `total_amount` decimal(10, 2) NOT NULL COMMENT '总积分，显示总账户值，记得一个人获得的总积分',
  `available_amount` decimal(10, 2) NOT NULL COMMENT '可用积分，每次扣减的值',
  `account_status` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账户状态【open - 可用，close - 冻结】',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 90 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户积分账户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_credit_account
-- ----------------------------
INSERT INTO `user_credit_account` VALUES (1, 'xiaofuge', 85.85, 85.85, 'open', '2024-05-24 22:11:59', '2024-06-09 10:45:11');
INSERT INTO `user_credit_account` VALUES (2, 'user003', 0.96, 0.96, 'open', '2024-05-25 10:53:20', '2024-05-25 10:54:31');
INSERT INTO `user_credit_account` VALUES (3, 'jay', 110.00, 110.00, 'open', '2025-09-11 15:34:41', '2025-09-11 16:07:59');
INSERT INTO `user_credit_account` VALUES (4, 'fvpki7.keb22', 202.80, 202.80, 'open', '2025-09-11 20:22:02', '2025-09-11 20:37:07');
INSERT INTO `user_credit_account` VALUES (5, 'ixjlj1_fgs', 109.00, 109.00, 'open', '2025-09-11 20:37:05', '2025-09-11 20:37:08');
INSERT INTO `user_credit_account` VALUES (6, 'v9bmdm98', 66.70, 66.70, 'open', '2025-09-11 20:37:06', '2025-09-11 20:37:06');
INSERT INTO `user_credit_account` VALUES (7, 'l7nrqi39', 82.80, 82.80, 'open', '2025-09-11 20:37:07', '2025-09-11 20:40:35');
INSERT INTO `user_credit_account` VALUES (8, 'tbfv9r_l7q84', 84.40, 84.40, 'open', '2025-09-11 20:37:08', '2025-09-11 20:37:08');
INSERT INTO `user_credit_account` VALUES (9, 'jwupu5.gjv', 5.93, 5.93, 'open', '2025-09-11 20:37:08', '2025-09-11 20:37:08');
INSERT INTO `user_credit_account` VALUES (10, 'jlbi46_jew', 67.00, 67.00, 'open', '2025-09-11 20:40:39', '2025-09-11 20:40:39');
INSERT INTO `user_credit_account` VALUES (11, 'k1zsbt76', 66.70, 66.70, 'open', '2025-09-11 20:40:41', '2025-09-11 20:40:41');
INSERT INTO `user_credit_account` VALUES (12, 'v9blnf12', 26.60, 26.60, 'open', '2025-09-11 20:40:41', '2025-09-11 20:40:41');
INSERT INTO `user_credit_account` VALUES (13, 'ggqryd_skd44', 41.90, 41.90, 'open', '2025-09-11 20:40:42', '2025-09-11 20:40:42');
INSERT INTO `user_credit_account` VALUES (14, 'fsekfk.jh485', 21.40, 21.40, 'open', '2025-09-11 20:40:43', '2025-09-11 20:40:43');
INSERT INTO `user_credit_account` VALUES (15, 'k8qkfk86', 63.00, 63.00, 'open', '2025-09-11 20:40:43', '2025-09-11 20:40:43');
INSERT INTO `user_credit_account` VALUES (16, 'lm1_p1k', 42.20, 42.20, 'open', '2025-09-11 20:40:45', '2025-09-11 20:40:45');
INSERT INTO `user_credit_account` VALUES (17, 'tgmo3v_tg9', 28.90, 28.90, 'open', '2025-09-11 20:40:46', '2025-09-11 20:40:46');
INSERT INTO `user_credit_account` VALUES (18, 'gs2m5650', 72.20, 72.20, 'open', '2025-09-11 20:40:48', '2025-09-11 20:40:48');
INSERT INTO `user_credit_account` VALUES (19, 'jzzgm341', 35.80, 35.80, 'open', '2025-09-11 20:40:49', '2025-09-11 20:40:49');
INSERT INTO `user_credit_account` VALUES (20, 'i2xmy224', 57.90, 57.90, 'open', '2025-09-11 20:40:50', '2025-09-11 20:40:50');
INSERT INTO `user_credit_account` VALUES (21, 'ryilj149', 79.30, 79.30, 'open', '2025-09-11 20:40:51', '2025-09-11 20:40:51');
INSERT INTO `user_credit_account` VALUES (22, 'm97k5q.ny968', 23.80, 23.80, 'open', '2025-09-11 20:40:55', '2025-09-11 20:40:55');
INSERT INTO `user_credit_account` VALUES (23, 'kgs_man44', 99.30, 99.30, 'open', '2025-09-11 20:40:58', '2025-09-11 20:40:58');
INSERT INTO `user_credit_account` VALUES (24, 'ts5hvb.skx', 67.00, 67.00, 'open', '2025-09-11 20:40:59', '2025-09-11 20:40:59');
INSERT INTO `user_credit_account` VALUES (25, 'tw6my286', 73.30, 73.30, 'open', '2025-09-11 20:41:04', '2025-09-11 20:41:04');
INSERT INTO `user_credit_account` VALUES (26, 'mawfmn82', 37.10, 37.10, 'open', '2025-09-11 20:41:06', '2025-09-11 20:41:06');
INSERT INTO `user_credit_account` VALUES (27, 'k5qkfk.qap8', 29.30, 29.30, 'open', '2025-09-11 20:41:10', '2025-09-11 20:41:10');
INSERT INTO `user_credit_account` VALUES (28, 'v9ru724', 90.50, 90.50, 'open', '2025-09-11 20:41:14', '2025-09-11 20:41:14');
INSERT INTO `user_credit_account` VALUES (29, 'k8qi48.pip39', 96.00, 96.00, 'open', '2025-09-11 20:41:17', '2025-09-11 20:41:17');
INSERT INTO `user_credit_account` VALUES (30, 'mzitue.m2hgkw', 81.00, 81.00, 'open', '2025-09-11 20:41:18', '2025-09-11 20:41:18');
INSERT INTO `user_credit_account` VALUES (31, 'gs2k34_ny930', 79.20, 79.20, 'open', '2025-09-11 20:41:21', '2025-09-11 20:41:21');
INSERT INTO `user_credit_account` VALUES (32, 'tbfnuy58', 40.30, 40.30, 'open', '2025-09-11 20:41:28', '2025-09-11 20:41:28');
INSERT INTO `user_credit_account` VALUES (33, 'ffxlmf_l9v84', 13.40, 13.40, 'open', '2025-09-11 20:41:28', '2025-09-11 20:41:28');
INSERT INTO `user_credit_account` VALUES (34, 'ttbtw992', 46.10, 46.10, 'open', '2025-09-11 20:41:31', '2025-09-11 20:41:31');
INSERT INTO `user_credit_account` VALUES (35, 'p72k3d.rvz', 84.60, 84.60, 'open', '2025-09-11 20:41:38', '2025-09-11 20:41:38');
INSERT INTO `user_credit_account` VALUES (36, 'jzzgm374', 71.20, 71.20, 'open', '2025-09-11 20:41:39', '2025-09-11 20:41:39');
INSERT INTO `user_credit_account` VALUES (37, 'lj1ljf_g8b10', 84.10, 84.10, 'open', '2025-09-11 20:41:41', '2025-09-11 20:41:41');
INSERT INTO `user_credit_account` VALUES (38, 'k1zggq.o34', 79.30, 79.30, 'open', '2025-09-11 20:41:43', '2025-09-11 20:41:43');
INSERT INTO `user_credit_account` VALUES (39, 'v9rmdm_gp82', 68.20, 68.20, 'open', '2025-09-11 20:41:43', '2025-09-11 20:41:43');
INSERT INTO `user_credit_account` VALUES (40, 'fextfv_hqj', 7.12, 7.12, 'open', '2025-09-11 20:41:46', '2025-09-11 20:41:46');
INSERT INTO `user_credit_account` VALUES (41, 'rmftt4.k28', 94.10, 94.10, 'open', '2025-09-11 20:41:51', '2025-09-11 20:41:51');
INSERT INTO `user_credit_account` VALUES (42, 'k8qscp97', 12.20, 12.20, 'open', '2025-09-11 20:41:52', '2025-09-11 20:41:52');
INSERT INTO `user_credit_account` VALUES (43, 'gztmft22', 6.46, 6.46, 'open', '2025-09-11 20:41:59', '2025-09-11 20:41:59');
INSERT INTO `user_credit_account` VALUES (44, 'oq3kgn_hge', 64.20, 64.20, 'open', '2025-09-11 20:42:01', '2025-09-11 20:42:01');
INSERT INTO `user_credit_account` VALUES (45, 'k7fv9r_fgw', 62.00, 62.00, 'open', '2025-09-11 20:42:02', '2025-09-11 20:42:02');
INSERT INTO `user_credit_account` VALUES (46, 'gs2m5656', 94.70, 94.70, 'open', '2025-09-11 20:42:03', '2025-09-11 20:42:03');
INSERT INTO `user_credit_account` VALUES (47, 'ttbtw9.i3e86', 48.30, 48.30, 'open', '2025-09-11 20:42:08', '2025-09-11 20:42:08');
INSERT INTO `user_credit_account` VALUES (48, 'izhlq4.oti', 27.10, 27.10, 'open', '2025-09-11 20:42:09', '2025-09-11 20:42:09');
INSERT INTO `user_credit_account` VALUES (49, 'lm1sds_rkx', 66.50, 66.50, 'open', '2025-09-11 20:42:10', '2025-09-11 20:42:10');
INSERT INTO `user_credit_account` VALUES (50, 'tt4lj1.fze59', 7.09, 7.09, 'open', '2025-09-11 20:42:13', '2025-09-11 20:42:13');
INSERT INTO `user_credit_account` VALUES (51, 'k8qkfk.p9c', 49.70, 49.70, 'open', '2025-09-11 20:42:13', '2025-09-11 20:42:13');
INSERT INTO `user_credit_account` VALUES (52, 'ttbtw9_gxn24', 52.10, 52.10, 'open', '2025-09-11 20:42:15', '2025-09-11 20:42:15');
INSERT INTO `user_credit_account` VALUES (53, 'v9rlnf_iwn40', 82.90, 82.90, 'open', '2025-09-11 20:42:16', '2025-09-11 20:42:16');
INSERT INTO `user_credit_account` VALUES (54, 'hm1i2v_sp2', 35.00, 35.00, 'open', '2025-09-11 20:42:21', '2025-09-11 20:42:21');
INSERT INTO `user_credit_account` VALUES (55, 'tdcsbt.sbq1', 84.20, 84.20, 'open', '2025-09-11 20:42:23', '2025-09-11 20:42:23');
INSERT INTO `user_credit_account` VALUES (56, 'mzitue.o2g87', 23.70, 23.70, 'open', '2025-09-11 20:42:31', '2025-09-11 20:42:31');
INSERT INTO `user_credit_account` VALUES (57, 'ui839', 29.30, 29.30, 'open', '2025-09-11 20:42:32', '2025-09-11 20:42:32');
INSERT INTO `user_credit_account` VALUES (58, 'lm1sbt6', 89.90, 89.90, 'open', '2025-09-11 20:42:36', '2025-09-11 20:42:36');
INSERT INTO `user_credit_account` VALUES (59, 'po4sf0_ib777', 28.90, 28.90, 'open', '2025-09-11 20:42:37', '2025-09-11 20:42:37');
INSERT INTO `user_credit_account` VALUES (60, 'ppm.nz1', 17.30, 17.30, 'open', '2025-09-11 20:42:39', '2025-09-11 20:42:39');
INSERT INTO `user_credit_account` VALUES (61, 'k88to3_kfc', 49.60, 49.60, 'open', '2025-09-11 20:42:40', '2025-09-11 20:42:40');
INSERT INTO `user_credit_account` VALUES (62, 'ucop8d.glk', 62.70, 62.70, 'open', '2025-09-11 20:42:44', '2025-09-11 20:42:44');
INSERT INTO `user_credit_account` VALUES (63, 'i1cuji52', 68.00, 68.00, 'open', '2025-09-11 20:42:49', '2025-09-11 20:42:49');
INSERT INTO `user_credit_account` VALUES (64, 'ts5tbf24', 61.40, 61.40, 'open', '2025-09-11 20:42:52', '2025-09-11 20:42:52');
INSERT INTO `user_credit_account` VALUES (65, 'lzbmd2.fqn', 31.90, 31.90, 'open', '2025-09-11 20:42:59', '2025-09-11 20:42:59');
INSERT INTO `user_credit_account` VALUES (66, 'o97kgs_ghb18', 75.40, 75.40, 'open', '2025-09-11 20:43:00', '2025-09-11 20:43:00');
INSERT INTO `user_credit_account` VALUES (67, 'tw6my276', 56.80, 56.80, 'open', '2025-09-11 20:43:03', '2025-09-11 20:43:03');
INSERT INTO `user_credit_account` VALUES (68, 'k8qi48_tml1', 57.60, 57.60, 'open', '2025-09-11 20:43:05', '2025-09-11 20:43:05');
INSERT INTO `user_credit_account` VALUES (69, 'ktzmzi_scp', 20.40, 20.40, 'open', '2025-09-11 20:43:07', '2025-09-11 20:43:07');
INSERT INTO `user_credit_account` VALUES (70, 'n0emzg_tn27', 71.20, 71.20, 'open', '2025-09-11 20:43:08', '2025-09-11 20:43:08');
INSERT INTO `user_credit_account` VALUES (71, 'k1zsbt_l9x91', 80.00, 80.00, 'open', '2025-09-11 20:43:09', '2025-09-11 20:43:09');
INSERT INTO `user_credit_account` VALUES (72, 'i1cl5v.ioq82', 21.30, 21.30, 'open', '2025-09-11 20:43:13', '2025-09-11 20:43:13');
INSERT INTO `user_credit_account` VALUES (73, 'hm1nuy_tt6', 22.20, 22.20, 'open', '2025-09-11 20:43:15', '2025-09-11 20:43:15');
INSERT INTO `user_credit_account` VALUES (74, 'ppm_sks30', 1.64, 1.64, 'open', '2025-09-11 20:43:17', '2025-09-11 20:43:17');
INSERT INTO `user_credit_account` VALUES (75, 'ffxlmf_lrs54', 1.13, 1.13, 'open', '2025-09-11 20:43:19', '2025-09-11 20:43:19');
INSERT INTO `user_credit_account` VALUES (76, 'ggqk1z31', 16.80, 16.80, 'open', '2025-09-11 20:43:20', '2025-09-11 20:43:20');
INSERT INTO `user_credit_account` VALUES (77, 'tw9izl_puf', 31.20, 31.20, 'open', '2025-09-11 20:43:27', '2025-09-11 20:43:27');
INSERT INTO `user_credit_account` VALUES (78, 'i1cvdk.sk766', 78.60, 78.60, 'open', '2025-09-11 20:43:28', '2025-09-11 20:43:28');
INSERT INTO `user_credit_account` VALUES (79, 'is8k1z_fgs', 51.20, 51.20, 'open', '2025-09-11 20:43:29', '2025-09-11 20:43:29');
INSERT INTO `user_credit_account` VALUES (80, 'oq3kgn.gl4o6y10', 52.50, 52.50, 'open', '2025-09-11 20:43:33', '2025-09-11 20:43:33');
INSERT INTO `user_credit_account` VALUES (81, 'k7ngvs.gky42', 72.50, 72.50, 'open', '2025-09-11 20:43:34', '2025-09-11 20:43:34');
INSERT INTO `user_credit_account` VALUES (82, 'lm1i2v63', 5.38, 5.38, 'open', '2025-09-11 20:43:36', '2025-09-11 20:43:36');
INSERT INTO `user_credit_account` VALUES (83, 'i1clo5_oti', 20.10, 20.10, 'open', '2025-09-11 20:43:37', '2025-09-11 20:43:37');
INSERT INTO `user_credit_account` VALUES (84, 'oq3kgn39', 15.60, 15.60, 'open', '2025-09-11 20:43:38', '2025-09-11 20:43:38');
INSERT INTO `user_credit_account` VALUES (85, 'fmnnzu.sib', 67.80, 67.80, 'open', '2025-09-11 20:43:39', '2025-09-11 20:43:39');
INSERT INTO `user_credit_account` VALUES (86, 'jzzgm3_ntz', 60.00, 60.00, 'open', '2025-09-11 20:43:46', '2025-09-11 20:43:46');
INSERT INTO `user_credit_account` VALUES (87, 'sf0ppm_onw', 11.60, 11.60, 'open', '2025-09-11 20:43:49', '2025-09-11 20:43:49');
INSERT INTO `user_credit_account` VALUES (88, 'hm1hs7_fep', 26.50, 26.50, 'open', '2025-09-11 20:43:51', '2025-09-11 20:43:51');
INSERT INTO `user_credit_account` VALUES (89, 'mzitue_lzw', 42.00, 42.00, 'open', '2025-09-11 20:43:51', '2025-09-11 20:43:51');

-- ----------------------------
-- Table structure for user_credit_order_000
-- ----------------------------
DROP TABLE IF EXISTS `user_credit_order_000`;
CREATE TABLE `user_credit_order_000`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单ID',
  `trade_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '交易名称',
  `trade_type` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'forward' COMMENT '交易类型；forward-正向、reverse-逆向',
  `trade_amount` decimal(10, 2) NOT NULL COMMENT '交易金额',
  `out_business_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务仿重ID - 外部透传。返利、行为等唯一标识',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  UNIQUE INDEX `uq_out_business_no`(`out_business_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户积分订单记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_credit_order_000
-- ----------------------------

-- ----------------------------
-- Table structure for user_credit_order_001
-- ----------------------------
DROP TABLE IF EXISTS `user_credit_order_001`;
CREATE TABLE `user_credit_order_001`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单ID',
  `trade_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '交易名称',
  `trade_type` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'forward' COMMENT '交易类型；forward-正向、reverse-逆向',
  `trade_amount` decimal(10, 2) NOT NULL COMMENT '交易金额',
  `out_business_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务仿重ID - 外部透传。返利、行为等唯一标识',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  UNIQUE INDEX `uq_out_business_no`(`out_business_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户积分订单记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_credit_order_001
-- ----------------------------

-- ----------------------------
-- Table structure for user_credit_order_002
-- ----------------------------
DROP TABLE IF EXISTS `user_credit_order_002`;
CREATE TABLE `user_credit_order_002`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单ID',
  `trade_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '交易名称',
  `trade_type` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'forward' COMMENT '交易类型；forward-正向、reverse-逆向',
  `trade_amount` decimal(10, 2) NOT NULL COMMENT '交易金额',
  `out_business_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务仿重ID - 外部透传。返利、行为等唯一标识',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  UNIQUE INDEX `uq_out_business_no`(`out_business_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户积分订单记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_credit_order_002
-- ----------------------------

-- ----------------------------
-- Table structure for user_credit_order_003
-- ----------------------------
DROP TABLE IF EXISTS `user_credit_order_003`;
CREATE TABLE `user_credit_order_003`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单ID',
  `trade_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '交易名称',
  `trade_type` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'forward' COMMENT '交易类型；forward-正向、reverse-逆向',
  `trade_amount` decimal(10, 2) NOT NULL COMMENT '交易金额',
  `out_business_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务仿重ID - 外部透传。返利、行为等唯一标识',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  UNIQUE INDEX `uq_out_business_no`(`out_business_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户积分订单记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_credit_order_003
-- ----------------------------

-- ----------------------------
-- Table structure for user_raffle_order_000
-- ----------------------------
DROP TABLE IF EXISTS `user_raffle_order_000`;
CREATE TABLE `user_raffle_order_000`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `activity_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动名称',
  `strategy_id` bigint NOT NULL COMMENT '抽奖策略ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单ID',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `order_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'create' COMMENT '订单状态；create-创建、used-已使用、cancel-已作废',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_user_id_activity_id`(`user_id` ASC, `activity_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 60 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户抽奖订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_raffle_order_000
-- ----------------------------
INSERT INTO `user_raffle_order_000` VALUES (1, 'p72k3d55', 100301, '测试活动', 100006, '249672253957', '2025-09-11 12:40:38', 'used', '2025-09-11 20:40:37', '2025-09-11 20:40:38');
INSERT INTO `user_raffle_order_000` VALUES (2, 'v9blnf12', 100301, '测试活动', 100006, '926235935225', '2025-09-11 12:40:41', 'used', '2025-09-11 20:40:41', '2025-09-11 20:40:41');
INSERT INTO `user_raffle_order_000` VALUES (3, 'k8qkfk86', 100301, '测试活动', 100006, '716151311315', '2025-09-11 12:40:43', 'used', '2025-09-11 20:40:43', '2025-09-11 20:40:43');
INSERT INTO `user_raffle_order_000` VALUES (4, 'irescp24', 100301, '测试活动', 100006, '262982027709', '2025-09-11 12:40:45', 'used', '2025-09-11 20:40:44', '2025-09-11 20:40:45');
INSERT INTO `user_raffle_order_000` VALUES (5, 'mzitue_ol733', 100301, '测试活动', 100006, '878501532647', '2025-09-11 12:40:52', 'used', '2025-09-11 20:40:52', '2025-09-11 20:40:52');
INSERT INTO `user_raffle_order_000` VALUES (6, 'ixjit6.laj61', 100301, '测试活动', 100006, '108813262747', '2025-09-11 12:40:54', 'used', '2025-09-11 20:40:54', '2025-09-11 20:40:54');
INSERT INTO `user_raffle_order_000` VALUES (7, 'oq3kgn38', 100301, '测试活动', 100006, '445209923164', '2025-09-11 12:40:57', 'used', '2025-09-11 20:40:57', '2025-09-11 20:40:57');
INSERT INTO `user_raffle_order_000` VALUES (8, 'kgs_man44', 100301, '测试活动', 100006, '651754461929', '2025-09-11 12:40:58', 'used', '2025-09-11 20:40:58', '2025-09-11 20:40:58');
INSERT INTO `user_raffle_order_000` VALUES (9, 'pa8kfk35', 100301, '测试活动', 100006, '278771929782', '2025-09-11 12:41:04', 'used', '2025-09-11 20:41:04', '2025-09-11 20:41:04');
INSERT INTO `user_raffle_order_000` VALUES (10, 'i1cvdk88', 100301, '测试活动', 100006, '997486405389', '2025-09-11 12:41:06', 'used', '2025-09-11 20:41:05', '2025-09-11 20:41:06');
INSERT INTO `user_raffle_order_000` VALUES (11, 'k5qkfk.qap8', 100301, '测试活动', 100006, '903258584118', '2025-09-11 12:41:10', 'used', '2025-09-11 20:41:10', '2025-09-11 20:41:10');
INSERT INTO `user_raffle_order_000` VALUES (12, 'k8qi48.pip39', 100301, '测试活动', 100006, '695425801466', '2025-09-11 12:41:17', 'used', '2025-09-11 20:41:17', '2025-09-11 20:41:17');
INSERT INTO `user_raffle_order_000` VALUES (13, 'lh7lnf96', 100301, '测试活动', 100006, '826087427643', '2025-09-11 12:41:19', 'used', '2025-09-11 20:41:19', '2025-09-11 20:41:19');
INSERT INTO `user_raffle_order_000` VALUES (14, 'fh5q31.ljf', 100301, '测试活动', 100006, '860452415786', '2025-09-11 12:41:20', 'used', '2025-09-11 20:41:20', '2025-09-11 20:41:20');
INSERT INTO `user_raffle_order_000` VALUES (15, 'gs2k34_ny930', 100301, '测试活动', 100006, '615093648577', '2025-09-11 12:41:21', 'used', '2025-09-11 20:41:21', '2025-09-11 20:41:21');
INSERT INTO `user_raffle_order_000` VALUES (16, 'tt4lj1_vix54', 100301, '测试活动', 100006, '516407902610', '2025-09-11 12:41:30', 'used', '2025-09-11 20:41:30', '2025-09-11 20:41:30');
INSERT INTO `user_raffle_order_000` VALUES (17, 'rydkjv.g3k46', 100301, '测试活动', 100006, '580317468716', '2025-09-11 12:41:33', 'used', '2025-09-11 20:41:33', '2025-09-11 20:41:33');
INSERT INTO `user_raffle_order_000` VALUES (18, 'p72k3d.rvz', 100301, '测试活动', 100006, '921974904299', '2025-09-11 12:41:38', 'used', '2025-09-11 20:41:37', '2025-09-11 20:41:38');
INSERT INTO `user_raffle_order_000` VALUES (19, 'o97scp88', 100301, '测试活动', 100006, '102545732985', '2025-09-11 12:41:44', 'used', '2025-09-11 20:41:44', '2025-09-11 20:41:44');
INSERT INTO `user_raffle_order_000` VALUES (20, 'izhlq423', 100301, '测试活动', 100006, '948359516153', '2025-09-11 12:41:45', 'used', '2025-09-11 20:41:45', '2025-09-11 20:41:45');
INSERT INTO `user_raffle_order_000` VALUES (21, 'p1pvgw84', 100301, '测试活动', 100006, '581273917437', '2025-09-11 12:41:47', 'used', '2025-09-11 20:41:46', '2025-09-11 20:41:46');
INSERT INTO `user_raffle_order_000` VALUES (22, 'k5mmcx_ny262', 100301, '测试活动', 100006, '893159250084', '2025-09-11 12:41:54', 'used', '2025-09-11 20:41:53', '2025-09-11 20:41:54');
INSERT INTO `user_raffle_order_000` VALUES (23, 'k18to3.lg016', 100301, '测试活动', 100006, '040552304849', '2025-09-11 12:41:55', 'used', '2025-09-11 20:41:54', '2025-09-11 20:41:55');
INSERT INTO `user_raffle_order_000` VALUES (24, 'o97scp33', 100301, '测试活动', 100006, '379793050041', '2025-09-11 12:41:56', 'used', '2025-09-11 20:41:56', '2025-09-11 20:41:56');
INSERT INTO `user_raffle_order_000` VALUES (25, 'ixjit6_tti32', 100301, '测试活动', 100006, '161907635283', '2025-09-11 12:41:59', 'used', '2025-09-11 20:41:59', '2025-09-11 20:41:59');
INSERT INTO `user_raffle_order_000` VALUES (26, 'k7fv9r_fgw', 100301, '测试活动', 100006, '192877495933', '2025-09-11 12:42:02', 'used', '2025-09-11 20:42:02', '2025-09-11 20:42:02');
INSERT INTO `user_raffle_order_000` VALUES (27, 'sf0ppm57', 100301, '测试活动', 100006, '190010576210', '2025-09-11 12:42:05', 'used', '2025-09-11 20:42:05', '2025-09-11 20:42:05');
INSERT INTO `user_raffle_order_000` VALUES (28, 'izhlq4.oti', 100301, '测试活动', 100006, '677122240868', '2025-09-11 12:42:09', 'used', '2025-09-11 20:42:08', '2025-09-11 20:42:09');
INSERT INTO `user_raffle_order_000` VALUES (29, 'v9r61', 100301, '测试活动', 100006, '065044793663', '2025-09-11 12:42:09', 'used', '2025-09-11 20:42:09', '2025-09-11 20:42:09');
INSERT INTO `user_raffle_order_000` VALUES (30, 'ryilj139', 100301, '测试活动', 100006, '459035556868', '2025-09-11 12:42:14', 'used', '2025-09-11 20:42:14', '2025-09-11 20:42:14');
INSERT INTO `user_raffle_order_000` VALUES (31, 'gs2m5680', 100301, '测试活动', 100006, '820604927800', '2025-09-11 12:42:19', 'used', '2025-09-11 20:42:19', '2025-09-11 20:42:19');
INSERT INTO `user_raffle_order_000` VALUES (32, 'hm1i2v_sp2', 100301, '测试活动', 100006, '166150736638', '2025-09-11 12:42:21', 'used', '2025-09-11 20:42:21', '2025-09-11 20:42:21');
INSERT INTO `user_raffle_order_000` VALUES (33, 'k5mmcx.jew', 100301, '测试活动', 100006, '844827963310', '2025-09-11 12:42:22', 'used', '2025-09-11 20:42:22', '2025-09-11 20:42:22');
INSERT INTO `user_raffle_order_000` VALUES (34, 'tbfnuy40', 100301, '测试活动', 100006, '844816569711', '2025-09-11 12:42:26', 'used', '2025-09-11 20:42:25', '2025-09-11 20:42:26');
INSERT INTO `user_raffle_order_000` VALUES (35, 'gs2m56_tqo', 100301, '测试活动', 100006, '681436236302', '2025-09-11 12:42:27', 'used', '2025-09-11 20:42:27', '2025-09-11 20:42:27');
INSERT INTO `user_raffle_order_000` VALUES (36, 'me4hce.nyg4', 100301, '测试活动', 100006, '164505502318', '2025-09-11 12:42:30', 'used', '2025-09-11 20:42:30', '2025-09-11 20:42:30');
INSERT INTO `user_raffle_order_000` VALUES (37, 'k8qkfk_ffg84', 100301, '测试活动', 100006, '475083682487', '2025-09-11 12:42:34', 'used', '2025-09-11 20:42:34', '2025-09-11 20:42:34');
INSERT INTO `user_raffle_order_000` VALUES (38, 'lm1sbt6', 100301, '测试活动', 100006, '586238808337', '2025-09-11 12:42:36', 'used', '2025-09-11 20:42:35', '2025-09-11 20:42:36');
INSERT INTO `user_raffle_order_000` VALUES (39, 'po4sf0_ib777', 100301, '测试活动', 100006, '815868729171', '2025-09-11 12:42:37', 'used', '2025-09-11 20:42:37', '2025-09-11 20:42:37');
INSERT INTO `user_raffle_order_000` VALUES (40, 'lmfmhh.ire77', 100301, '测试活动', 100006, '803199541856', '2025-09-11 12:42:38', 'used', '2025-09-11 20:42:38', '2025-09-11 20:42:38');
INSERT INTO `user_raffle_order_000` VALUES (41, 'ucop8d.glk', 100301, '测试活动', 100006, '086628206446', '2025-09-11 12:42:44', 'used', '2025-09-11 20:42:44', '2025-09-11 20:42:44');
INSERT INTO `user_raffle_order_000` VALUES (42, 'ryilj186', 100301, '测试活动', 100006, '018611415498', '2025-09-11 12:42:45', 'used', '2025-09-11 20:42:45', '2025-09-11 20:42:45');
INSERT INTO `user_raffle_order_000` VALUES (43, 'k7ngvs_gbf', 100301, '测试活动', 100006, '919983311697', '2025-09-11 12:42:48', 'used', '2025-09-11 20:42:47', '2025-09-11 20:42:48');
INSERT INTO `user_raffle_order_000` VALUES (44, 'gs2m56_ggl72', 100301, '测试活动', 100006, '846235838309', '2025-09-11 12:42:54', 'used', '2025-09-11 20:42:54', '2025-09-11 20:42:54');
INSERT INTO `user_raffle_order_000` VALUES (45, 'g6nmzi_irk', 100301, '测试活动', 100006, '277454856421', '2025-09-11 12:43:01', 'used', '2025-09-11 20:43:00', '2025-09-11 20:43:00');
INSERT INTO `user_raffle_order_000` VALUES (46, 'mzitue_nj340', 100301, '测试活动', 100006, '906419520345', '2025-09-11 12:43:04', 'used', '2025-09-11 20:43:03', '2025-09-11 20:43:04');
INSERT INTO `user_raffle_order_000` VALUES (47, 'k8qi48_tml1', 100301, '测试活动', 100006, '811233753965', '2025-09-11 12:43:05', 'used', '2025-09-11 20:43:05', '2025-09-11 20:43:05');
INSERT INTO `user_raffle_order_000` VALUES (48, 'k18iui_tn253', 100301, '测试活动', 100006, '184983286687', '2025-09-11 12:43:11', 'used', '2025-09-11 20:43:11', '2025-09-11 20:43:11');
INSERT INTO `user_raffle_order_000` VALUES (49, 'rydkjv_qj262', 100301, '测试活动', 100006, '231057891225', '2025-09-11 12:43:13', 'used', '2025-09-11 20:43:12', '2025-09-11 20:43:13');
INSERT INTO `user_raffle_order_000` VALUES (50, 'v9rmdm75', 100301, '测试活动', 100006, '822067077994', '2025-09-11 12:43:14', 'used', '2025-09-11 20:43:14', '2025-09-11 20:43:14');
INSERT INTO `user_raffle_order_000` VALUES (51, 'hm1nuy_tt6', 100301, '测试活动', 100006, '542128510344', '2025-09-11 12:43:15', 'used', '2025-09-11 20:43:15', '2025-09-11 20:43:15');
INSERT INTO `user_raffle_order_000` VALUES (52, 'ppm_sks30', 100301, '测试活动', 100006, '603113690564', '2025-09-11 12:43:17', 'used', '2025-09-11 20:43:17', '2025-09-11 20:43:17');
INSERT INTO `user_raffle_order_000` VALUES (53, 'ffxlmf_lrs54', 100301, '测试活动', 100006, '960946217886', '2025-09-11 12:43:19', 'used', '2025-09-11 20:43:19', '2025-09-11 20:43:19');
INSERT INTO `user_raffle_order_000` VALUES (54, 'lh7k1z_i46', 100301, '测试活动', 100006, '803498049674', '2025-09-11 12:43:23', 'used', '2025-09-11 20:43:22', '2025-09-11 20:43:22');
INSERT INTO `user_raffle_order_000` VALUES (55, 'gs2k34_u4f88', 100301, '测试活动', 100006, '126225669348', '2025-09-11 12:43:31', 'used', '2025-09-11 20:43:30', '2025-09-11 20:43:30');
INSERT INTO `user_raffle_order_000` VALUES (56, 'k5mmcx.qg973', 100301, '测试活动', 100006, '821081860164', '2025-09-11 12:43:32', 'used', '2025-09-11 20:43:32', '2025-09-11 20:43:32');
INSERT INTO `user_raffle_order_000` VALUES (57, 'ffxlmf_gqg33', 100301, '测试活动', 100006, '083921769796', '2025-09-11 12:43:35', 'used', '2025-09-11 20:43:34', '2025-09-11 20:43:35');
INSERT INTO `user_raffle_order_000` VALUES (58, 'k18iui23', 100301, '测试活动', 100006, '626574273186', '2025-09-11 12:43:39', 'used', '2025-09-11 20:43:39', '2025-09-11 20:43:39');
INSERT INTO `user_raffle_order_000` VALUES (59, 'jlbffg20', 100301, '测试活动', 100006, '323886762199', '2025-09-11 12:43:43', 'used', '2025-09-11 20:43:43', '2025-09-11 20:43:43');

-- ----------------------------
-- Table structure for user_raffle_order_001
-- ----------------------------
DROP TABLE IF EXISTS `user_raffle_order_001`;
CREATE TABLE `user_raffle_order_001`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `activity_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动名称',
  `strategy_id` bigint NOT NULL COMMENT '抽奖策略ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单ID',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `order_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'create' COMMENT '订单状态；create-创建、used-已使用、cancel-已作废',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_user_id_activity_id`(`user_id` ASC, `activity_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 211 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户抽奖订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_raffle_order_001
-- ----------------------------
INSERT INTO `user_raffle_order_001` VALUES (128, 'fvpki7.keb22', 100301, '测试活动', 100006, '152137500941', '2025-09-11 12:22:02', 'used', '2025-09-11 20:22:02', '2025-09-11 20:22:02');
INSERT INTO `user_raffle_order_001` VALUES (129, 'fvpki7.keb22', 100301, '测试活动', 100006, '605504311002', '2025-09-11 12:22:09', 'used', '2025-09-11 20:22:09', '2025-09-11 20:22:09');
INSERT INTO `user_raffle_order_001` VALUES (130, 'fvpki7.keb22', 100301, '测试活动', 100006, '450114916627', '2025-09-11 12:22:14', 'used', '2025-09-11 20:22:14', '2025-09-11 20:22:14');
INSERT INTO `user_raffle_order_001` VALUES (131, 'fvpki7.keb22', 100301, '测试活动', 100006, '912745270715', '2025-09-11 12:37:04', 'used', '2025-09-11 20:37:04', '2025-09-11 20:37:04');
INSERT INTO `user_raffle_order_001` VALUES (132, 'fvpki7.keb22', 100301, '测试活动', 100006, '749956198335', '2025-09-11 12:37:06', 'used', '2025-09-11 20:37:05', '2025-09-11 20:37:06');
INSERT INTO `user_raffle_order_001` VALUES (133, 'fvpki7.keb22', 100301, '测试活动', 100006, '040211232269', '2025-09-11 12:37:06', 'used', '2025-09-11 20:37:06', '2025-09-11 20:37:06');
INSERT INTO `user_raffle_order_001` VALUES (134, 'v9bmdm98', 100301, '测试活动', 100006, '736666802256', '2025-09-11 12:37:06', 'used', '2025-09-11 20:37:06', '2025-09-11 20:37:06');
INSERT INTO `user_raffle_order_001` VALUES (135, 'fvpki7.keb22', 100301, '测试活动', 100006, '245170135024', '2025-09-11 12:37:07', 'used', '2025-09-11 20:37:07', '2025-09-11 20:37:07');
INSERT INTO `user_raffle_order_001` VALUES (136, 'tbfv9r_l7q84', 100301, '测试活动', 100006, '417206744050', '2025-09-11 12:37:08', 'used', '2025-09-11 20:37:07', '2025-09-11 20:37:08');
INSERT INTO `user_raffle_order_001` VALUES (137, 'jwupu5.gjv', 100301, '测试活动', 100006, '133003370970', '2025-09-11 12:37:08', 'used', '2025-09-11 20:37:08', '2025-09-11 20:37:08');
INSERT INTO `user_raffle_order_001` VALUES (138, 'v9bmdm98', 100301, '测试活动', 100006, '110775777610', '2025-09-11 12:37:09', 'used', '2025-09-11 20:37:08', '2025-09-11 20:37:09');
INSERT INTO `user_raffle_order_001` VALUES (139, 'fvpki7.keb22', 100301, '测试活动', 100006, '892818350905', '2025-09-11 12:40:33', 'used', '2025-09-11 20:40:32', '2025-09-11 20:40:33');
INSERT INTO `user_raffle_order_001` VALUES (140, 'v9bmdm98', 100301, '测试活动', 100006, '356166292511', '2025-09-11 12:40:34', 'used', '2025-09-11 20:40:34', '2025-09-11 20:40:34');
INSERT INTO `user_raffle_order_001` VALUES (141, 'tbfv9r_l7q84', 100301, '测试活动', 100006, '225304149056', '2025-09-11 12:40:36', 'used', '2025-09-11 20:40:35', '2025-09-11 20:40:35');
INSERT INTO `user_raffle_order_001` VALUES (142, 'jwupu5.gjv', 100301, '测试活动', 100006, '752808039912', '2025-09-11 12:40:36', 'used', '2025-09-11 20:40:36', '2025-09-11 20:40:36');
INSERT INTO `user_raffle_order_001` VALUES (143, 'k1zsbt76', 100301, '测试活动', 100006, '756591068309', '2025-09-11 12:40:40', 'used', '2025-09-11 20:40:40', '2025-09-11 20:40:40');
INSERT INTO `user_raffle_order_001` VALUES (144, 'tgmo3v_tg9', 100301, '测试活动', 100006, '670748640780', '2025-09-11 12:40:46', 'used', '2025-09-11 20:40:46', '2025-09-11 20:40:46');
INSERT INTO `user_raffle_order_001` VALUES (145, 'sf0ppm47', 100301, '测试活动', 100006, '871712532191', '2025-09-11 12:40:47', 'used', '2025-09-11 20:40:46', '2025-09-11 20:40:47');
INSERT INTO `user_raffle_order_001` VALUES (146, 'jzzgm341', 100301, '测试活动', 100006, '966825698801', '2025-09-11 12:40:49', 'used', '2025-09-11 20:40:48', '2025-09-11 20:40:49');
INSERT INTO `user_raffle_order_001` VALUES (147, 'i2xmy224', 100301, '测试活动', 100006, '713708837505', '2025-09-11 12:40:50', 'used', '2025-09-11 20:40:50', '2025-09-11 20:40:50');
INSERT INTO `user_raffle_order_001` VALUES (148, 'nlrlqi_j7y', 100301, '测试活动', 100006, '313538968950', '2025-09-11 12:40:51', 'used', '2025-09-11 20:40:50', '2025-09-11 20:40:51');
INSERT INTO `user_raffle_order_001` VALUES (149, 'ryilj149', 100301, '测试活动', 100006, '148928701524', '2025-09-11 12:40:51', 'used', '2025-09-11 20:40:51', '2025-09-11 20:40:51');
INSERT INTO `user_raffle_order_001` VALUES (150, 'ixjlj1_tqo', 100301, '测试活动', 100006, '871633686147', '2025-09-11 12:40:53', 'used', '2025-09-11 20:40:52', '2025-09-11 20:40:53');
INSERT INTO `user_raffle_order_001` VALUES (151, 'ui8_n1l68', 100301, '测试活动', 100006, '063523150482', '2025-09-11 12:40:55', 'used', '2025-09-11 20:40:55', '2025-09-11 20:40:55');
INSERT INTO `user_raffle_order_001` VALUES (152, 'me4nzu_ph932', 100301, '测试活动', 100006, '649329124724', '2025-09-11 12:41:08', 'used', '2025-09-11 20:41:07', '2025-09-11 20:41:08');
INSERT INTO `user_raffle_order_001` VALUES (153, 'tbfv9r_ssq', 100301, '测试活动', 100006, '806787329592', '2025-09-11 12:41:08', 'used', '2025-09-11 20:41:08', '2025-09-11 20:41:08');
INSERT INTO `user_raffle_order_001` VALUES (154, 'pu9ggq.gc3', 100301, '测试活动', 100006, '698154511241', '2025-09-11 12:41:11', 'used', '2025-09-11 20:41:11', '2025-09-11 20:41:11');
INSERT INTO `user_raffle_order_001` VALUES (155, 'rmftt492', 100301, '测试活动', 100006, '392430616332', '2025-09-11 12:41:12', 'used', '2025-09-11 20:41:11', '2025-09-11 20:41:12');
INSERT INTO `user_raffle_order_001` VALUES (156, 'ui859', 100301, '测试活动', 100006, '495875507119', '2025-09-11 12:41:13', 'used', '2025-09-11 20:41:13', '2025-09-11 20:41:13');
INSERT INTO `user_raffle_order_001` VALUES (157, 'hm1nuy43', 100301, '测试活动', 100006, '530579532664', '2025-09-11 12:41:15', 'used', '2025-09-11 20:41:14', '2025-09-11 20:41:15');
INSERT INTO `user_raffle_order_001` VALUES (158, 'nlrlqi66', 100301, '测试活动', 100006, '771204990342', '2025-09-11 12:41:15', 'used', '2025-09-11 20:41:15', '2025-09-11 20:41:15');
INSERT INTO `user_raffle_order_001` VALUES (159, 'tgmo3v.tfa99', 100301, '测试活动', 100006, '482088172491', '2025-09-11 12:41:25', 'used', '2025-09-11 20:41:25', '2025-09-11 20:41:25');
INSERT INTO `user_raffle_order_001` VALUES (160, 'fozmy264', 100301, '测试活动', 100006, '579074268560', '2025-09-11 12:41:32', 'used', '2025-09-11 20:41:32', '2025-09-11 20:41:32');
INSERT INTO `user_raffle_order_001` VALUES (161, 'fgwuih10', 100301, '测试活动', 100006, '044003661381', '2025-09-11 12:41:34', 'used', '2025-09-11 20:41:33', '2025-09-11 20:41:34');
INSERT INTO `user_raffle_order_001` VALUES (162, 'gs2m5634', 100301, '测试活动', 100006, '811524536799', '2025-09-11 12:41:35', 'used', '2025-09-11 20:41:34', '2025-09-11 20:41:35');
INSERT INTO `user_raffle_order_001` VALUES (163, 'jzzgm374', 100301, '测试活动', 100006, '495836124725', '2025-09-11 12:41:39', 'used', '2025-09-11 20:41:39', '2025-09-11 20:41:39');
INSERT INTO `user_raffle_order_001` VALUES (164, 'i1cuji.nev32', 100301, '测试活动', 100006, '225776565928', '2025-09-11 12:41:40', 'used', '2025-09-11 20:41:40', '2025-09-11 20:41:40');
INSERT INTO `user_raffle_order_001` VALUES (165, 'hm1nuy.lqd', 100301, '测试活动', 100006, '782987856019', '2025-09-11 12:41:42', 'used', '2025-09-11 20:41:42', '2025-09-11 20:41:42');
INSERT INTO `user_raffle_order_001` VALUES (166, 'k1zggq.o34', 100301, '测试活动', 100006, '852715642756', '2025-09-11 12:41:43', 'used', '2025-09-11 20:41:42', '2025-09-11 20:41:43');
INSERT INTO `user_raffle_order_001` VALUES (167, 'fextfv_hqj', 100301, '测试活动', 100006, '940049978778', '2025-09-11 12:41:46', 'used', '2025-09-11 20:41:46', '2025-09-11 20:41:46');
INSERT INTO `user_raffle_order_001` VALUES (168, 'iuk.ptd72', 100301, '测试活动', 100006, '219005873316', '2025-09-11 12:41:47', 'used', '2025-09-11 20:41:47', '2025-09-11 20:41:47');
INSERT INTO `user_raffle_order_001` VALUES (169, 'p72k3d43', 100301, '测试活动', 100006, '921294224927', '2025-09-11 12:41:50', 'used', '2025-09-11 20:41:49', '2025-09-11 20:41:50');
INSERT INTO `user_raffle_order_001` VALUES (170, 'gztmft22', 100301, '测试活动', 100006, '769143912994', '2025-09-11 12:41:59', 'used', '2025-09-11 20:41:58', '2025-09-11 20:41:59');
INSERT INTO `user_raffle_order_001` VALUES (171, 'oq3kgn_hge', 100301, '测试活动', 100006, '385794861024', '2025-09-11 12:42:01', 'used', '2025-09-11 20:42:01', '2025-09-11 20:42:01');
INSERT INTO `user_raffle_order_001` VALUES (172, 'gs2m5656', 100301, '测试活动', 100006, '908488826853', '2025-09-11 12:42:03', 'used', '2025-09-11 20:42:02', '2025-09-11 20:42:03');
INSERT INTO `user_raffle_order_001` VALUES (173, 'ucop8d_i4y87', 100301, '测试活动', 100006, '204482846618', '2025-09-11 12:42:04', 'used', '2025-09-11 20:42:04', '2025-09-11 20:42:04');
INSERT INTO `user_raffle_order_001` VALUES (174, 'k88to334', 100301, '测试活动', 100006, '447012892329', '2025-09-11 12:42:07', 'used', '2025-09-11 20:42:07', '2025-09-11 20:42:07');
INSERT INTO `user_raffle_order_001` VALUES (175, 'lm1sds_rkx', 100301, '测试活动', 100006, '540735345140', '2025-09-11 12:42:10', 'used', '2025-09-11 20:42:10', '2025-09-11 20:42:10');
INSERT INTO `user_raffle_order_001` VALUES (176, 'o97scp_i0k11', 100301, '测试活动', 100006, '371400129964', '2025-09-11 12:42:17', 'used', '2025-09-11 20:42:17', '2025-09-11 20:42:17');
INSERT INTO `user_raffle_order_001` VALUES (177, 'k7znlr.s3r', 100301, '测试活动', 100006, '635597877347', '2025-09-11 12:42:18', 'used', '2025-09-11 20:42:18', '2025-09-11 20:42:18');
INSERT INTO `user_raffle_order_001` VALUES (178, 'fmni4832', 100301, '测试活动', 100006, '224937399918', '2025-09-11 12:42:20', 'used', '2025-09-11 20:42:20', '2025-09-11 20:42:20');
INSERT INTO `user_raffle_order_001` VALUES (179, 'o97scp87', 100301, '测试活动', 100006, '224062467529', '2025-09-11 12:42:21', 'used', '2025-09-11 20:42:20', '2025-09-11 20:42:21');
INSERT INTO `user_raffle_order_001` VALUES (180, 'tdcsbt.sbq1', 100301, '测试活动', 100006, '862019663400', '2025-09-11 12:42:23', 'used', '2025-09-11 20:42:22', '2025-09-11 20:42:23');
INSERT INTO `user_raffle_order_001` VALUES (181, 'fvpki7.ghc52', 100301, '测试活动', 100006, '521007419974', '2025-09-11 12:42:25', 'used', '2025-09-11 20:42:25', '2025-09-11 20:42:25');
INSERT INTO `user_raffle_order_001` VALUES (182, 'gs2k34_i3h8', 100301, '测试活动', 100006, '338432256895', '2025-09-11 12:42:28', 'used', '2025-09-11 20:42:28', '2025-09-11 20:42:28');
INSERT INTO `user_raffle_order_001` VALUES (183, 'mzitue.o2g87', 100301, '测试活动', 100006, '393834428885', '2025-09-11 12:42:31', 'used', '2025-09-11 20:42:30', '2025-09-11 20:42:31');
INSERT INTO `user_raffle_order_001` VALUES (184, 'ggqryd44', 100301, '测试活动', 100006, '756770617554', '2025-09-11 12:42:42', 'used', '2025-09-11 20:42:41', '2025-09-11 20:42:42');
INSERT INTO `user_raffle_order_001` VALUES (185, 'tt6kfy.g4v78', 100301, '测试活动', 100006, '453671620406', '2025-09-11 12:42:43', 'used', '2025-09-11 20:42:43', '2025-09-11 20:42:43');
INSERT INTO `user_raffle_order_001` VALUES (186, 'o97fmn21', 100301, '测试活动', 100006, '816061191703', '2025-09-11 12:42:47', 'used', '2025-09-11 20:42:47', '2025-09-11 20:42:47');
INSERT INTO `user_raffle_order_001` VALUES (187, 'i1cuji52', 100301, '测试活动', 100006, '120726740982', '2025-09-11 12:42:49', 'used', '2025-09-11 20:42:49', '2025-09-11 20:42:49');
INSERT INTO `user_raffle_order_001` VALUES (188, 'lmfmhh_sl5', 100301, '测试活动', 100006, '033518193799', '2025-09-11 12:42:51', 'used', '2025-09-11 20:42:51', '2025-09-11 20:42:51');
INSERT INTO `user_raffle_order_001` VALUES (189, 'fevfoz.lpi22', 100301, '测试活动', 100006, '351834030681', '2025-09-11 12:42:53', 'used', '2025-09-11 20:42:53', '2025-09-11 20:42:53');
INSERT INTO `user_raffle_order_001` VALUES (190, 'izhpd6_iwd81', 100301, '测试活动', 100006, '052387100699', '2025-09-11 12:42:57', 'used', '2025-09-11 20:42:57', '2025-09-11 20:42:57');
INSERT INTO `user_raffle_order_001` VALUES (191, 'jcvsbt.fh869', 100301, '测试活动', 100006, '104174547688', '2025-09-11 12:42:58', 'used', '2025-09-11 20:42:58', '2025-09-11 20:42:58');
INSERT INTO `user_raffle_order_001` VALUES (192, 'o97kgs_ghb18', 100301, '测试活动', 100006, '403554336722', '2025-09-11 12:43:00', 'used', '2025-09-11 20:42:59', '2025-09-11 20:43:00');
INSERT INTO `user_raffle_order_001` VALUES (193, 'tw6my276', 100301, '测试活动', 100006, '071068426153', '2025-09-11 12:43:03', 'used', '2025-09-11 20:43:03', '2025-09-11 20:43:03');
INSERT INTO `user_raffle_order_001` VALUES (194, 'n0emzg_tn27', 100301, '测试活动', 100006, '503860844003', '2025-09-11 12:43:08', 'used', '2025-09-11 20:43:08', '2025-09-11 20:43:08');
INSERT INTO `user_raffle_order_001` VALUES (195, 'k1zsbt_l9x91', 100301, '测试活动', 100006, '075525008239', '2025-09-11 12:43:09', 'used', '2025-09-11 20:43:08', '2025-09-11 20:43:09');
INSERT INTO `user_raffle_order_001` VALUES (196, 'k18iui_ist', 100301, '测试活动', 100006, '462978645860', '2025-09-11 12:43:12', 'used', '2025-09-11 20:43:11', '2025-09-11 20:43:12');
INSERT INTO `user_raffle_order_001` VALUES (197, 'i1cl5v.ioq82', 100301, '测试活动', 100006, '523223098524', '2025-09-11 12:43:13', 'used', '2025-09-11 20:43:13', '2025-09-11 20:43:13');
INSERT INTO `user_raffle_order_001` VALUES (198, 'ts5hvb_ssq', 100301, '测试活动', 100006, '852348661153', '2025-09-11 12:43:21', 'used', '2025-09-11 20:43:20', '2025-09-11 20:43:21');
INSERT INTO `user_raffle_order_001` VALUES (199, 'tw9izl_puf', 100301, '测试活动', 100006, '000636486649', '2025-09-11 12:43:27', 'used', '2025-09-11 20:43:27', '2025-09-11 20:43:27');
INSERT INTO `user_raffle_order_001` VALUES (200, 'i1cvdk.sk766', 100301, '测试活动', 100006, '567337063279', '2025-09-11 12:43:28', 'used', '2025-09-11 20:43:28', '2025-09-11 20:43:28');
INSERT INTO `user_raffle_order_001` VALUES (201, 'oq3kgn.gl4o6y10', 100301, '测试活动', 100006, '315521815970', '2025-09-11 12:43:33', 'used', '2025-09-11 20:43:33', '2025-09-11 20:43:33');
INSERT INTO `user_raffle_order_001` VALUES (202, 'lm1i2v63', 100301, '测试活动', 100006, '604611442316', '2025-09-11 12:43:36', 'used', '2025-09-11 20:43:36', '2025-09-11 20:43:36');
INSERT INTO `user_raffle_order_001` VALUES (203, 'oq3kgn39', 100301, '测试活动', 100006, '549524966738', '2025-09-11 12:43:38', 'used', '2025-09-11 20:43:37', '2025-09-11 20:43:38');
INSERT INTO `user_raffle_order_001` VALUES (204, 'k1zu0c_skj34', 100301, '测试活动', 100006, '040699385942', '2025-09-11 12:43:42', 'used', '2025-09-11 20:43:42', '2025-09-11 20:43:42');
INSERT INTO `user_raffle_order_001` VALUES (205, 'v9bmdm.s3r72', 100301, '测试活动', 100006, '152214386602', '2025-09-11 12:43:45', 'used', '2025-09-11 20:43:45', '2025-09-11 20:43:45');
INSERT INTO `user_raffle_order_001` VALUES (206, 'sf0ppm_onw', 100301, '测试活动', 100006, '353513340001', '2025-09-11 12:43:49', 'used', '2025-09-11 20:43:48', '2025-09-11 20:43:49');
INSERT INTO `user_raffle_order_001` VALUES (207, 'u6mgge_sfi', 100301, '测试活动', 100006, '522475732741', '2025-09-11 12:43:50', 'used', '2025-09-11 20:43:49', '2025-09-11 20:43:50');
INSERT INTO `user_raffle_order_001` VALUES (208, 'hm1hs7_fep', 100301, '测试活动', 100006, '094317938876', '2025-09-11 12:43:50', 'used', '2025-09-11 20:43:50', '2025-09-11 20:43:50');
INSERT INTO `user_raffle_order_001` VALUES (209, 'mzitue_lzw', 100301, '测试活动', 100006, '513797830139', '2025-09-11 12:43:51', 'used', '2025-09-11 20:43:51', '2025-09-11 20:43:51');
INSERT INTO `user_raffle_order_001` VALUES (210, 'me4nzu15', 100301, '测试活动', 100006, '618605856813', '2025-09-11 12:43:54', 'used', '2025-09-11 20:43:53', '2025-09-11 20:43:54');

-- ----------------------------
-- Table structure for user_raffle_order_002
-- ----------------------------
DROP TABLE IF EXISTS `user_raffle_order_002`;
CREATE TABLE `user_raffle_order_002`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `activity_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动名称',
  `strategy_id` bigint NOT NULL COMMENT '抽奖策略ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单ID',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `order_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'create' COMMENT '订单状态；create-创建、used-已使用、cancel-已作废',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_user_id_activity_id`(`user_id` ASC, `activity_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 95 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户抽奖订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_raffle_order_002
-- ----------------------------
INSERT INTO `user_raffle_order_002` VALUES (17, 'ixjlj1_fgs', 100301, '测试活动', 100006, '485358948528', '2025-09-11 12:37:05', 'used', '2025-09-11 20:37:05', '2025-09-11 20:37:05');
INSERT INTO `user_raffle_order_002` VALUES (18, 'ixjlj1_fgs', 100301, '测试活动', 100006, '548043642799', '2025-09-11 12:37:07', 'used', '2025-09-11 20:37:07', '2025-09-11 20:37:07');
INSERT INTO `user_raffle_order_002` VALUES (19, 'ixjlj1_fgs', 100301, '测试活动', 100006, '532429947610', '2025-09-11 12:37:07', 'used', '2025-09-11 20:37:07', '2025-09-11 20:37:08');
INSERT INTO `user_raffle_order_002` VALUES (20, 'ixjlj1_fgs', 100301, '测试活动', 100006, '524147420844', '2025-09-11 12:37:07', 'used', '2025-09-11 20:37:07', '2025-09-11 20:37:08');
INSERT INTO `user_raffle_order_002` VALUES (21, 'ixjlj1_fgs', 100301, '测试活动', 100006, '138453371004', '2025-09-11 12:37:08', 'used', '2025-09-11 20:37:07', '2025-09-11 20:37:08');
INSERT INTO `user_raffle_order_002` VALUES (22, 'ixjlj1_fgs', 100301, '测试活动', 100006, '112183785106', '2025-09-11 12:37:08', 'used', '2025-09-11 20:37:08', '2025-09-11 20:37:08');
INSERT INTO `user_raffle_order_002` VALUES (23, 'ixjlj1_fgs', 100301, '测试活动', 100006, '499395995020', '2025-09-11 12:40:33', 'used', '2025-09-11 20:40:33', '2025-09-11 20:40:33');
INSERT INTO `user_raffle_order_002` VALUES (24, 'izhfoz52', 100301, '测试活动', 100006, '256087726548', '2025-09-11 12:40:38', 'used', '2025-09-11 20:40:38', '2025-09-11 20:40:38');
INSERT INTO `user_raffle_order_002` VALUES (25, 'jlbi46_jew', 100301, '测试活动', 100006, '178433616575', '2025-09-11 12:40:39', 'used', '2025-09-11 20:40:39', '2025-09-11 20:40:39');
INSERT INTO `user_raffle_order_002` VALUES (26, 'vdak5x51', 100301, '测试活动', 100006, '221950821442', '2025-09-11 12:40:40', 'used', '2025-09-11 20:40:39', '2025-09-11 20:40:40');
INSERT INTO `user_raffle_order_002` VALUES (27, 'fsekfk.jh485', 100301, '测试活动', 100006, '777944065189', '2025-09-11 12:40:43', 'used', '2025-09-11 20:40:42', '2025-09-11 20:40:42');
INSERT INTO `user_raffle_order_002` VALUES (28, 'pd6fvp_kd762', 100301, '测试活动', 100006, '690852255757', '2025-09-11 12:40:44', 'used', '2025-09-11 20:40:44', '2025-09-11 20:40:44');
INSERT INTO `user_raffle_order_002` VALUES (29, 'k8qscp_glx72', 100301, '测试活动', 100006, '972070713546', '2025-09-11 12:40:49', 'used', '2025-09-11 20:40:49', '2025-09-11 20:40:49');
INSERT INTO `user_raffle_order_002` VALUES (30, 'm97k5q.ny968', 100301, '测试活动', 100006, '322136064600', '2025-09-11 12:40:55', 'used', '2025-09-11 20:40:54', '2025-09-11 20:40:55');
INSERT INTO `user_raffle_order_002` VALUES (31, 'k1z74', 100301, '测试活动', 100006, '017668200517', '2025-09-11 12:40:57', 'used', '2025-09-11 20:40:56', '2025-09-11 20:40:57');
INSERT INTO `user_raffle_order_002` VALUES (32, 'i1csbt_gqo91', 100301, '测试活动', 100006, '650808405297', '2025-09-11 12:40:59', 'used', '2025-09-11 20:40:58', '2025-09-11 20:40:59');
INSERT INTO `user_raffle_order_002` VALUES (33, 'ts5hvb.skx', 100301, '测试活动', 100006, '531965810993', '2025-09-11 12:40:59', 'used', '2025-09-11 20:40:59', '2025-09-11 20:40:59');
INSERT INTO `user_raffle_order_002` VALUES (34, 'm8eiuk_fl0', 100301, '测试活动', 100006, '140757662837', '2025-09-11 12:41:00', 'used', '2025-09-11 20:41:00', '2025-09-11 20:41:00');
INSERT INTO `user_raffle_order_002` VALUES (35, 'k8qkfk.sks1', 100301, '测试活动', 100006, '161973468187', '2025-09-11 12:41:01', 'used', '2025-09-11 20:41:00', '2025-09-11 20:41:01');
INSERT INTO `user_raffle_order_002` VALUES (36, 'ui865', 100301, '测试活动', 100006, '807771818632', '2025-09-11 12:41:01', 'used', '2025-09-11 20:41:01', '2025-09-11 20:41:01');
INSERT INTO `user_raffle_order_002` VALUES (37, 'tw6my286', 100301, '测试活动', 100006, '458886356058', '2025-09-11 12:41:04', 'used', '2025-09-11 20:41:03', '2025-09-11 20:41:04');
INSERT INTO `user_raffle_order_002` VALUES (38, 'tt6kfy.i809', 100301, '测试活动', 100006, '501392725475', '2025-09-11 12:41:05', 'used', '2025-09-11 20:41:05', '2025-09-11 20:41:05');
INSERT INTO `user_raffle_order_002` VALUES (39, 'k5mhm1_ky2', 100301, '测试活动', 100006, '478297588215', '2025-09-11 12:41:10', 'used', '2025-09-11 20:41:09', '2025-09-11 20:41:10');
INSERT INTO `user_raffle_order_002` VALUES (40, 'tt6kfy.lly', 100301, '测试活动', 100006, '583573290486', '2025-09-11 12:41:12', 'used', '2025-09-11 20:41:12', '2025-09-11 20:41:12');
INSERT INTO `user_raffle_order_002` VALUES (41, 'fevfoz.gne23', 100301, '测试活动', 100006, '617071776299', '2025-09-11 12:41:16', 'used', '2025-09-11 20:41:16', '2025-09-11 20:41:16');
INSERT INTO `user_raffle_order_002` VALUES (42, 'mzitue.m2hgkw', 100301, '测试活动', 100006, '206722639797', '2025-09-11 12:41:18', 'used', '2025-09-11 20:41:17', '2025-09-11 20:41:18');
INSERT INTO `user_raffle_order_002` VALUES (43, 'pu9ggq_h5q81', 100301, '测试活动', 100006, '973860844310', '2025-09-11 12:41:23', 'used', '2025-09-11 20:41:23', '2025-09-11 20:41:23');
INSERT INTO `user_raffle_order_002` VALUES (44, 'hm1nuy_frz89', 100301, '测试活动', 100006, '730843364839', '2025-09-11 12:41:24', 'used', '2025-09-11 20:41:24', '2025-09-11 20:41:24');
INSERT INTO `user_raffle_order_002` VALUES (45, 'k1zu0c.gbf58', 100301, '测试活动', 100006, '521343929271', '2025-09-11 12:41:26', 'used', '2025-09-11 20:41:26', '2025-09-11 20:41:26');
INSERT INTO `user_raffle_order_002` VALUES (46, 'id7mft99', 100301, '测试活动', 100006, '854020727435', '2025-09-11 12:41:27', 'used', '2025-09-11 20:41:26', '2025-09-11 20:41:27');
INSERT INTO `user_raffle_order_002` VALUES (47, 'fmnrmi_ptd', 100301, '测试活动', 100006, '084327458304', '2025-09-11 12:41:29', 'used', '2025-09-11 20:41:29', '2025-09-11 20:41:29');
INSERT INTO `user_raffle_order_002` VALUES (48, 'ttbtw992', 100301, '测试活动', 100006, '617734339171', '2025-09-11 12:41:31', 'used', '2025-09-11 20:41:31', '2025-09-11 20:41:31');
INSERT INTO `user_raffle_order_002` VALUES (49, 'mawnuy3', 100301, '测试活动', 100006, '291889375230', '2025-09-11 12:41:35', 'used', '2025-09-11 20:41:35', '2025-09-11 20:41:35');
INSERT INTO `user_raffle_order_002` VALUES (50, 'izhfoz96', 100301, '测试活动', 100006, '313042747935', '2025-09-11 12:41:37', 'used', '2025-09-11 20:41:37', '2025-09-11 20:41:37');
INSERT INTO `user_raffle_order_002` VALUES (51, 'lj1ljf_g8b10', 100301, '测试活动', 100006, '351644873053', '2025-09-11 12:41:41', 'used', '2025-09-11 20:41:41', '2025-09-11 20:41:41');
INSERT INTO `user_raffle_order_002` VALUES (52, 'jwui2v_n5e', 100301, '测试活动', 100006, '908380651223', '2025-09-11 12:41:48', 'used', '2025-09-11 20:41:48', '2025-09-11 20:41:48');
INSERT INTO `user_raffle_order_002` VALUES (53, 'i1cvdk31', 100301, '测试活动', 100006, '562073825721', '2025-09-11 12:41:51', 'used', '2025-09-11 20:41:51', '2025-09-11 20:41:51');
INSERT INTO `user_raffle_order_002` VALUES (54, 'k5mit6.fk780', 100301, '测试活动', 100006, '615737078424', '2025-09-11 12:41:57', 'used', '2025-09-11 20:41:57', '2025-09-11 20:41:57');
INSERT INTO `user_raffle_order_002` VALUES (55, 'k5q_io462', 100301, '测试活动', 100006, '485424893384', '2025-09-11 12:41:58', 'used', '2025-09-11 20:41:58', '2025-09-11 20:41:58');
INSERT INTO `user_raffle_order_002` VALUES (56, 'fevfoz.kfz23', 100301, '测试活动', 100006, '665781895326', '2025-09-11 12:42:00', 'used', '2025-09-11 20:42:00', '2025-09-11 20:42:00');
INSERT INTO `user_raffle_order_002` VALUES (57, 'nlrlqi.p1k', 100301, '测试活动', 100006, '757896962943', '2025-09-11 12:42:04', 'used', '2025-09-11 20:42:03', '2025-09-11 20:42:04');
INSERT INTO `user_raffle_order_002` VALUES (58, 'ttbtw9.i3e86', 100301, '测试活动', 100006, '488067867408', '2025-09-11 12:42:08', 'used', '2025-09-11 20:42:07', '2025-09-11 20:42:08');
INSERT INTO `user_raffle_order_002` VALUES (59, 'knnk88.n5fi8w64', 100301, '测试活动', 100006, '643114980878', '2025-09-11 12:42:12', 'used', '2025-09-11 20:42:11', '2025-09-11 20:42:12');
INSERT INTO `user_raffle_order_002` VALUES (60, 'tt4lj1.fze59', 100301, '测试活动', 100006, '319856419701', '2025-09-11 12:42:13', 'used', '2025-09-11 20:42:12', '2025-09-11 20:42:12');
INSERT INTO `user_raffle_order_002` VALUES (61, 'k8qkfk.p9c', 100301, '测试活动', 100006, '419561181762', '2025-09-11 12:42:13', 'used', '2025-09-11 20:42:13', '2025-09-11 20:42:13');
INSERT INTO `user_raffle_order_002` VALUES (62, 'ttbtw9_gxn24', 100301, '测试活动', 100006, '716035662307', '2025-09-11 12:42:15', 'used', '2025-09-11 20:42:15', '2025-09-11 20:42:15');
INSERT INTO `user_raffle_order_002` VALUES (63, 'fh5q31_irk', 100301, '测试活动', 100006, '763116763002', '2025-09-11 12:42:23', 'used', '2025-09-11 20:42:23', '2025-09-11 20:42:23');
INSERT INTO `user_raffle_order_002` VALUES (64, 'lh7lnf69', 100301, '测试活动', 100006, '526985160531', '2025-09-11 12:42:24', 'used', '2025-09-11 20:42:24', '2025-09-11 20:42:24');
INSERT INTO `user_raffle_order_002` VALUES (65, 'k5mmcx.n5fi8w', 100301, '测试活动', 100006, '737976416703', '2025-09-11 12:42:27', 'used', '2025-09-11 20:42:26', '2025-09-11 20:42:27');
INSERT INTO `user_raffle_order_002` VALUES (66, 'k88to3.tqj', 100301, '测试活动', 100006, '115092865111', '2025-09-11 12:42:31', 'used', '2025-09-11 20:42:31', '2025-09-11 20:42:31');
INSERT INTO `user_raffle_order_002` VALUES (67, 'lnf_ggu', 100301, '测试活动', 100006, '977866713299', '2025-09-11 12:42:33', 'used', '2025-09-11 20:42:33', '2025-09-11 20:42:33');
INSERT INTO `user_raffle_order_002` VALUES (68, 'vacsbt97', 100301, '测试活动', 100006, '349139001591', '2025-09-11 12:42:36', 'used', '2025-09-11 20:42:36', '2025-09-11 20:42:36');
INSERT INTO `user_raffle_order_002` VALUES (69, 'k88to3_kfc', 100301, '测试活动', 100006, '428683916432', '2025-09-11 12:42:40', 'used', '2025-09-11 20:42:40', '2025-09-11 20:42:40');
INSERT INTO `user_raffle_order_002` VALUES (70, 'sf0ppm.k2860', 100301, '测试活动', 100006, '729071627449', '2025-09-11 12:42:41', 'used', '2025-09-11 20:42:41', '2025-09-11 20:42:41');
INSERT INTO `user_raffle_order_002` VALUES (71, 'pybsbt30', 100301, '测试活动', 100006, '171704560181', '2025-09-11 12:42:43', 'used', '2025-09-11 20:42:42', '2025-09-11 20:42:42');
INSERT INTO `user_raffle_order_002` VALUES (72, 'ggqryd_i6hsf355', 100301, '测试活动', 100006, '812926632238', '2025-09-11 12:42:48', 'used', '2025-09-11 20:42:48', '2025-09-11 20:42:48');
INSERT INTO `user_raffle_order_002` VALUES (73, 'pybsbt85', 100301, '测试活动', 100006, '200778888041', '2025-09-11 12:42:50', 'used', '2025-09-11 20:42:50', '2025-09-11 20:42:50');
INSERT INTO `user_raffle_order_002` VALUES (74, 'ts5tbf24', 100301, '测试活动', 100006, '677289723362', '2025-09-11 12:42:52', 'used', '2025-09-11 20:42:51', '2025-09-11 20:42:52');
INSERT INTO `user_raffle_order_002` VALUES (75, 'mzitue_sn143', 100301, '测试活动', 100006, '291153056326', '2025-09-11 12:42:52', 'used', '2025-09-11 20:42:52', '2025-09-11 20:42:52');
INSERT INTO `user_raffle_order_002` VALUES (76, 'mzitue.tcu0', 100301, '测试活动', 100006, '901646401115', '2025-09-11 12:42:55', 'used', '2025-09-11 20:42:55', '2025-09-11 20:42:55');
INSERT INTO `user_raffle_order_002` VALUES (77, 'izhsf0_go824', 100301, '测试活动', 100006, '275910238047', '2025-09-11 12:42:56', 'used', '2025-09-11 20:42:55', '2025-09-11 20:42:56');
INSERT INTO `user_raffle_order_002` VALUES (78, 'lzbmd2.fqn', 100301, '测试活动', 100006, '319761523912', '2025-09-11 12:42:59', 'used', '2025-09-11 20:42:59', '2025-09-11 20:42:59');
INSERT INTO `user_raffle_order_002` VALUES (79, 'pd6fvp_ssq', 100301, '测试活动', 100006, '639930962915', '2025-09-11 12:43:02', 'used', '2025-09-11 20:43:02', '2025-09-11 20:43:02');
INSERT INTO `user_raffle_order_002` VALUES (80, 'pa8kfk6', 100301, '测试活动', 100006, '812651958318', '2025-09-11 12:43:05', 'used', '2025-09-11 20:43:04', '2025-09-11 20:43:05');
INSERT INTO `user_raffle_order_002` VALUES (81, 'm8eiuk_g3f', 100301, '测试活动', 100006, '501632111531', '2025-09-11 12:43:06', 'used', '2025-09-11 20:43:06', '2025-09-11 20:43:06');
INSERT INTO `user_raffle_order_002` VALUES (82, 'ktzmzi_scp', 100301, '测试活动', 100006, '017015492394', '2025-09-11 12:43:07', 'used', '2025-09-11 20:43:07', '2025-09-11 20:43:07');
INSERT INTO `user_raffle_order_002` VALUES (83, 'k5qsbt.oar93', 100301, '测试活动', 100006, '034321735675', '2025-09-11 12:43:10', 'used', '2025-09-11 20:43:10', '2025-09-11 20:43:10');
INSERT INTO `user_raffle_order_002` VALUES (84, 'hm1i2v26', 100301, '测试活动', 100006, '746457778173', '2025-09-11 12:43:18', 'used', '2025-09-11 20:43:18', '2025-09-11 20:43:18');
INSERT INTO `user_raffle_order_002` VALUES (85, 'pybsbt.fnq', 100301, '测试活动', 100006, '595087657700', '2025-09-11 12:43:24', 'used', '2025-09-11 20:43:24', '2025-09-11 20:43:24');
INSERT INTO `user_raffle_order_002` VALUES (86, 'gs2k34_nez', 100301, '测试活动', 100006, '431030825356', '2025-09-11 12:43:26', 'used', '2025-09-11 20:43:26', '2025-09-11 20:43:26');
INSERT INTO `user_raffle_order_002` VALUES (87, 'is8k1z_fgs', 100301, '测试活动', 100006, '804363396279', '2025-09-11 12:43:29', 'used', '2025-09-11 20:43:29', '2025-09-11 20:43:29');
INSERT INTO `user_raffle_order_002` VALUES (88, 'k7ngvs.gky42', 100301, '测试活动', 100006, '250530534925', '2025-09-11 12:43:34', 'used', '2025-09-11 20:43:33', '2025-09-11 20:43:34');
INSERT INTO `user_raffle_order_002` VALUES (89, 'vacsbt86', 100301, '测试活动', 100006, '442279039053', '2025-09-11 12:43:44', 'used', '2025-09-11 20:43:44', '2025-09-11 20:43:44');
INSERT INTO `user_raffle_order_002` VALUES (90, 'jzzgm3_ntz', 100301, '测试活动', 100006, '597975380190', '2025-09-11 12:43:46', 'used', '2025-09-11 20:43:45', '2025-09-11 20:43:46');
INSERT INTO `user_raffle_order_002` VALUES (91, 'gztmft.i85', 100301, '测试活动', 100006, '476854803183', '2025-09-11 12:43:47', 'used', '2025-09-11 20:43:47', '2025-09-11 20:43:47');
INSERT INTO `user_raffle_order_002` VALUES (92, 'izhlu8_kgs62', 100301, '测试活动', 100006, '699283069554', '2025-09-11 12:43:48', 'used', '2025-09-11 20:43:48', '2025-09-11 20:43:48');
INSERT INTO `user_raffle_order_002` VALUES (93, 'ttbtw9_hlm32', 100301, '测试活动', 100006, '561448789939', '2025-09-11 12:43:53', 'used', '2025-09-11 20:43:53', '2025-09-11 20:43:53');
INSERT INTO `user_raffle_order_002` VALUES (94, 'lm1_tti', 100301, '测试活动', 100006, '119920536560', '2025-09-11 12:43:54', 'used', '2025-09-11 20:43:54', '2025-09-11 20:43:54');

-- ----------------------------
-- Table structure for user_raffle_order_003
-- ----------------------------
DROP TABLE IF EXISTS `user_raffle_order_003`;
CREATE TABLE `user_raffle_order_003`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `activity_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动名称',
  `strategy_id` bigint NOT NULL COMMENT '抽奖策略ID',
  `order_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单ID',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `order_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'create' COMMENT '订单状态；create-创建、used-已使用、cancel-已作废',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_user_id_activity_id`(`user_id` ASC, `activity_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 168 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户抽奖订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_raffle_order_003
-- ----------------------------
INSERT INTO `user_raffle_order_003` VALUES (2, 'Jay', 100301, '测试活动', 100006, '123251889509', '2025-09-11 08:17:20', 'used', '2025-09-11 16:17:20', '2025-09-11 16:18:01');
INSERT INTO `user_raffle_order_003` VALUES (3, 'Jay', 100301, '测试活动', 100006, '047265279257', '2025-09-11 08:18:11', 'used', '2025-09-11 16:18:11', '2025-09-11 16:18:12');
INSERT INTO `user_raffle_order_003` VALUES (4, 'jay', 100301, '测试活动', 100006, '391754657215', '2025-09-11 08:20:55', 'used', '2025-09-11 16:20:55', '2025-09-11 16:20:56');
INSERT INTO `user_raffle_order_003` VALUES (5, 'jay', 100301, '测试活动', 100006, '076933735647', '2025-09-11 08:23:21', 'used', '2025-09-11 16:23:21', '2025-09-11 16:23:21');
INSERT INTO `user_raffle_order_003` VALUES (6, 'jay', 100301, '测试活动', 100006, '783289721604', '2025-09-11 08:23:37', 'used', '2025-09-11 16:23:37', '2025-09-11 16:23:38');
INSERT INTO `user_raffle_order_003` VALUES (7, 'jay', 100301, '测试活动', 100006, '033552351982', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:04', '2025-09-11 16:24:04');
INSERT INTO `user_raffle_order_003` VALUES (8, 'jay', 100301, '测试活动', 100006, '821015638921', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:04', '2025-09-11 16:24:04');
INSERT INTO `user_raffle_order_003` VALUES (9, 'jay', 100301, '测试活动', 100006, '046267938929', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:04', '2025-09-11 16:24:04');
INSERT INTO `user_raffle_order_003` VALUES (10, 'jay', 100301, '测试活动', 100006, '499159553296', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:04', '2025-09-11 16:24:04');
INSERT INTO `user_raffle_order_003` VALUES (11, 'jay', 100301, '测试活动', 100006, '297927773450', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:04', '2025-09-11 16:24:05');
INSERT INTO `user_raffle_order_003` VALUES (12, 'jay', 100301, '测试活动', 100006, '216637128225', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:04', '2025-09-11 16:24:05');
INSERT INTO `user_raffle_order_003` VALUES (13, 'jay', 100301, '测试活动', 100006, '205945313683', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:05', '2025-09-11 16:24:05');
INSERT INTO `user_raffle_order_003` VALUES (14, 'jay', 100301, '测试活动', 100006, '757591364360', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:05', '2025-09-11 16:24:05');
INSERT INTO `user_raffle_order_003` VALUES (15, 'jay', 100301, '测试活动', 100006, '571613897942', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:05', '2025-09-11 16:24:05');
INSERT INTO `user_raffle_order_003` VALUES (16, 'jay', 100301, '测试活动', 100006, '546832500103', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:05', '2025-09-11 16:24:06');
INSERT INTO `user_raffle_order_003` VALUES (17, 'jay', 100301, '测试活动', 100006, '238802359406', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:05', '2025-09-11 16:24:06');
INSERT INTO `user_raffle_order_003` VALUES (18, 'jay', 100301, '测试活动', 100006, '646916945448', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:05', '2025-09-11 16:24:06');
INSERT INTO `user_raffle_order_003` VALUES (19, 'jay', 100301, '测试活动', 100006, '194147973545', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:06', '2025-09-11 16:24:06');
INSERT INTO `user_raffle_order_003` VALUES (20, 'jay', 100301, '测试活动', 100006, '983748839600', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:06', '2025-09-11 16:24:06');
INSERT INTO `user_raffle_order_003` VALUES (21, 'jay', 100301, '测试活动', 100006, '615094360243', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:06', '2025-09-11 16:24:06');
INSERT INTO `user_raffle_order_003` VALUES (22, 'jay', 100301, '测试活动', 100006, '652310164400', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:06', '2025-09-11 16:24:06');
INSERT INTO `user_raffle_order_003` VALUES (23, 'jay', 100301, '测试活动', 100006, '562470757638', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:06', '2025-09-11 16:24:07');
INSERT INTO `user_raffle_order_003` VALUES (24, 'jay', 100301, '测试活动', 100006, '006241983283', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:06', '2025-09-11 16:24:07');
INSERT INTO `user_raffle_order_003` VALUES (25, 'jay', 100301, '测试活动', 100006, '552393467348', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:07', '2025-09-11 16:24:07');
INSERT INTO `user_raffle_order_003` VALUES (26, 'jay', 100301, '测试活动', 100006, '971520963966', '2025-09-11 08:24:04', 'used', '2025-09-11 16:24:07', '2025-09-11 16:24:07');
INSERT INTO `user_raffle_order_003` VALUES (27, 'jay', 100301, '测试活动', 100006, '783707444418', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:07', '2025-09-11 16:24:08');
INSERT INTO `user_raffle_order_003` VALUES (28, 'jay', 100301, '测试活动', 100006, '441429068493', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:07', '2025-09-11 16:24:08');
INSERT INTO `user_raffle_order_003` VALUES (29, 'jay', 100301, '测试活动', 100006, '237545059288', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `user_raffle_order_003` VALUES (30, 'jay', 100301, '测试活动', 100006, '390428969669', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `user_raffle_order_003` VALUES (31, 'jay', 100301, '测试活动', 100006, '523135197581', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `user_raffle_order_003` VALUES (32, 'jay', 100301, '测试活动', 100006, '443766981895', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `user_raffle_order_003` VALUES (33, 'jay', 100301, '测试活动', 100006, '961659922796', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:08', '2025-09-11 16:24:08');
INSERT INTO `user_raffle_order_003` VALUES (34, 'jay', 100301, '测试活动', 100006, '091048707105', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:08', '2025-09-11 16:24:09');
INSERT INTO `user_raffle_order_003` VALUES (35, 'jay', 100301, '测试活动', 100006, '669079823424', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:09', '2025-09-11 16:24:09');
INSERT INTO `user_raffle_order_003` VALUES (36, 'jay', 100301, '测试活动', 100006, '659624201810', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:09', '2025-09-11 16:24:09');
INSERT INTO `user_raffle_order_003` VALUES (37, 'jay', 100301, '测试活动', 100006, '283040982965', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:09', '2025-09-11 16:24:09');
INSERT INTO `user_raffle_order_003` VALUES (38, 'jay', 100301, '测试活动', 100006, '812669656316', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:09', '2025-09-11 16:24:09');
INSERT INTO `user_raffle_order_003` VALUES (39, 'jay', 100301, '测试活动', 100006, '063456093801', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:09', '2025-09-11 16:24:09');
INSERT INTO `user_raffle_order_003` VALUES (40, 'jay', 100301, '测试活动', 100006, '307575448159', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:09', '2025-09-11 16:24:10');
INSERT INTO `user_raffle_order_003` VALUES (41, 'jay', 100301, '测试活动', 100006, '872514825381', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:09', '2025-09-11 16:24:10');
INSERT INTO `user_raffle_order_003` VALUES (42, 'jay', 100301, '测试活动', 100006, '867998269250', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:10', '2025-09-11 16:24:10');
INSERT INTO `user_raffle_order_003` VALUES (43, 'jay', 100301, '测试活动', 100006, '455875088253', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:10', '2025-09-11 16:24:10');
INSERT INTO `user_raffle_order_003` VALUES (44, 'jay', 100301, '测试活动', 100006, '431923721932', '2025-09-11 08:24:07', 'used', '2025-09-11 16:24:10', '2025-09-11 16:24:10');
INSERT INTO `user_raffle_order_003` VALUES (45, 'jay', 100301, '测试活动', 100006, '526176379196', '2025-09-11 08:24:08', 'used', '2025-09-11 16:24:10', '2025-09-11 16:24:10');
INSERT INTO `user_raffle_order_003` VALUES (46, 'jay', 100301, '测试活动', 100006, '133844269407', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:47', '2025-09-11 16:25:48');
INSERT INTO `user_raffle_order_003` VALUES (47, 'jay', 100301, '测试活动', 100006, '539334579100', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:47', '2025-09-11 16:25:48');
INSERT INTO `user_raffle_order_003` VALUES (48, 'jay', 100301, '测试活动', 100006, '114307413504', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:47', '2025-09-11 16:25:48');
INSERT INTO `user_raffle_order_003` VALUES (49, 'jay', 100301, '测试活动', 100006, '793842208878', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:47', '2025-09-11 16:25:48');
INSERT INTO `user_raffle_order_003` VALUES (50, 'jay', 100301, '测试活动', 100006, '071707115347', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:48', '2025-09-11 16:25:48');
INSERT INTO `user_raffle_order_003` VALUES (51, 'jay', 100301, '测试活动', 100006, '160343840537', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:48', '2025-09-11 16:25:48');
INSERT INTO `user_raffle_order_003` VALUES (52, 'jay', 100301, '测试活动', 100006, '781957534904', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:48', '2025-09-11 16:25:48');
INSERT INTO `user_raffle_order_003` VALUES (53, 'jay', 100301, '测试活动', 100006, '847007343147', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:48', '2025-09-11 16:25:48');
INSERT INTO `user_raffle_order_003` VALUES (54, 'jay', 100301, '测试活动', 100006, '333682254849', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:48', '2025-09-11 16:25:49');
INSERT INTO `user_raffle_order_003` VALUES (55, 'jay', 100301, '测试活动', 100006, '558331798435', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:48', '2025-09-11 16:25:49');
INSERT INTO `user_raffle_order_003` VALUES (56, 'jay', 100301, '测试活动', 100006, '026648834930', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:49', '2025-09-11 16:25:49');
INSERT INTO `user_raffle_order_003` VALUES (57, 'jay', 100301, '测试活动', 100006, '743509727445', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:49', '2025-09-11 16:25:49');
INSERT INTO `user_raffle_order_003` VALUES (58, 'jay', 100301, '测试活动', 100006, '074378267714', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:49', '2025-09-11 16:25:49');
INSERT INTO `user_raffle_order_003` VALUES (59, 'jay', 100301, '测试活动', 100006, '508985175657', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:49', '2025-09-11 16:25:49');
INSERT INTO `user_raffle_order_003` VALUES (60, 'jay', 100301, '测试活动', 100006, '549839403269', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:49', '2025-09-11 16:25:50');
INSERT INTO `user_raffle_order_003` VALUES (61, 'jay', 100301, '测试活动', 100006, '070452517982', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:49', '2025-09-11 16:25:50');
INSERT INTO `user_raffle_order_003` VALUES (62, 'jay', 100301, '测试活动', 100006, '932629270157', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:50', '2025-09-11 16:25:50');
INSERT INTO `user_raffle_order_003` VALUES (63, 'jay', 100301, '测试活动', 100006, '831208651493', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:50', '2025-09-11 16:25:50');
INSERT INTO `user_raffle_order_003` VALUES (64, 'jay', 100301, '测试活动', 100006, '755053385182', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:50', '2025-09-11 16:25:50');
INSERT INTO `user_raffle_order_003` VALUES (65, 'jay', 100301, '测试活动', 100006, '516887892627', '2025-09-11 08:25:47', 'used', '2025-09-11 16:25:50', '2025-09-11 16:25:50');
INSERT INTO `user_raffle_order_003` VALUES (66, 'jay', 100301, '测试活动', 100006, '479504085878', '2025-09-11 08:25:48', 'used', '2025-09-11 16:25:50', '2025-09-11 16:25:51');
INSERT INTO `user_raffle_order_003` VALUES (67, 'jay', 100301, '测试活动', 100006, '768119144839', '2025-09-11 08:25:48', 'used', '2025-09-11 16:25:50', '2025-09-11 16:25:51');
INSERT INTO `user_raffle_order_003` VALUES (68, 'jay', 100301, '测试活动', 100006, '144414259715', '2025-09-11 08:25:48', 'used', '2025-09-11 16:25:51', '2025-09-11 16:25:51');
INSERT INTO `user_raffle_order_003` VALUES (69, 'jay', 100301, '测试活动', 100006, '773169473027', '2025-09-11 08:25:48', 'used', '2025-09-11 16:25:51', '2025-09-11 16:25:51');
INSERT INTO `user_raffle_order_003` VALUES (70, 'jay', 100301, '测试活动', 100006, '065288892123', '2025-09-11 08:25:48', 'used', '2025-09-11 16:25:51', '2025-09-11 16:25:51');
INSERT INTO `user_raffle_order_003` VALUES (71, 'jay', 100301, '测试活动', 100006, '985109117801', '2025-09-11 08:25:48', 'used', '2025-09-11 16:25:51', '2025-09-11 16:25:52');
INSERT INTO `user_raffle_order_003` VALUES (72, 'jay', 100301, '测试活动', 100006, '783215776478', '2025-09-11 08:25:48', 'used', '2025-09-11 16:25:51', '2025-09-11 16:25:52');
INSERT INTO `user_raffle_order_003` VALUES (73, 'jay', 100301, '测试活动', 100006, '839870176735', '2025-09-11 08:25:49', 'used', '2025-09-11 16:25:51', '2025-09-11 16:25:52');
INSERT INTO `user_raffle_order_003` VALUES (74, 'jay', 100301, '测试活动', 100006, '664037492884', '2025-09-11 08:25:49', 'used', '2025-09-11 16:25:52', '2025-09-11 16:25:52');
INSERT INTO `user_raffle_order_003` VALUES (75, 'jay', 100301, '测试活动', 100006, '586084309178', '2025-09-11 08:25:49', 'used', '2025-09-11 16:25:52', '2025-09-11 16:25:52');
INSERT INTO `user_raffle_order_003` VALUES (76, 'jay', 100301, '测试活动', 100006, '956670727490', '2025-09-11 08:25:49', 'used', '2025-09-11 16:25:52', '2025-09-11 16:25:52');
INSERT INTO `user_raffle_order_003` VALUES (77, 'jay', 100301, '测试活动', 100006, '031398518710', '2025-09-11 08:25:49', 'used', '2025-09-11 16:25:52', '2025-09-11 16:25:52');
INSERT INTO `user_raffle_order_003` VALUES (78, 'jay', 100301, '测试活动', 100006, '915593783833', '2025-09-11 08:25:49', 'used', '2025-09-11 16:25:52', '2025-09-11 16:25:53');
INSERT INTO `user_raffle_order_003` VALUES (79, 'jay', 100301, '测试活动', 100006, '102342243297', '2025-09-11 08:25:50', 'used', '2025-09-11 16:25:52', '2025-09-11 16:25:53');
INSERT INTO `user_raffle_order_003` VALUES (80, 'jay', 100301, '测试活动', 100006, '174948679152', '2025-09-11 08:25:50', 'used', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `user_raffle_order_003` VALUES (81, 'jay', 100301, '测试活动', 100006, '455316181452', '2025-09-11 08:25:50', 'used', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `user_raffle_order_003` VALUES (82, 'jay', 100301, '测试活动', 100006, '165163642098', '2025-09-11 08:25:50', 'used', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `user_raffle_order_003` VALUES (83, 'jay', 100301, '测试活动', 100006, '258394248391', '2025-09-11 08:25:50', 'used', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `user_raffle_order_003` VALUES (84, 'jay', 100301, '测试活动', 100006, '514988530785', '2025-09-11 08:25:51', 'used', '2025-09-11 16:25:53', '2025-09-11 16:25:53');
INSERT INTO `user_raffle_order_003` VALUES (85, 'jay', 100301, '测试活动', 100006, '460369076599', '2025-09-11 08:25:51', 'used', '2025-09-11 16:25:53', '2025-09-11 16:25:54');
INSERT INTO `user_raffle_order_003` VALUES (86, 'jay', 100301, '测试活动', 100006, '723454370226', '2025-09-11 08:25:51', 'used', '2025-09-11 16:25:53', '2025-09-11 16:25:54');
INSERT INTO `user_raffle_order_003` VALUES (87, 'jay', 100301, '测试活动', 100006, '902258464786', '2025-09-11 08:25:51', 'used', '2025-09-11 16:25:54', '2025-09-11 16:25:54');
INSERT INTO `user_raffle_order_003` VALUES (88, 'jay', 100301, '测试活动', 100006, '224233858952', '2025-09-11 08:25:51', 'used', '2025-09-11 16:25:54', '2025-09-11 16:25:54');
INSERT INTO `user_raffle_order_003` VALUES (89, 'jay', 100301, '测试活动', 100006, '960866059786', '2025-09-11 08:25:51', 'used', '2025-09-11 16:25:54', '2025-09-11 16:25:54');
INSERT INTO `user_raffle_order_003` VALUES (90, 'jay', 100301, '测试活动', 100006, '361355765275', '2025-09-11 08:25:52', 'used', '2025-09-11 16:25:54', '2025-09-11 16:25:54');
INSERT INTO `user_raffle_order_003` VALUES (91, 'jay', 100301, '测试活动', 100006, '885952284004', '2025-09-11 08:25:52', 'used', '2025-09-11 16:25:54', '2025-09-11 16:25:55');
INSERT INTO `user_raffle_order_003` VALUES (92, 'jay', 100301, '测试活动', 100006, '083192581036', '2025-09-11 08:25:52', 'used', '2025-09-11 16:25:54', '2025-09-11 16:25:55');
INSERT INTO `user_raffle_order_003` VALUES (93, 'jay', 100301, '测试活动', 100006, '971260585952', '2025-09-11 11:57:59', 'used', '2025-09-11 19:57:59', '2025-09-11 19:57:59');
INSERT INTO `user_raffle_order_003` VALUES (94, 'jay', 100301, '测试活动', 100006, '349104647196', '2025-09-11 12:16:06', 'used', '2025-09-11 20:16:06', '2025-09-11 20:16:06');
INSERT INTO `user_raffle_order_003` VALUES (95, 'jay', 100301, '测试活动', 100006, '520284234489', '2025-09-11 12:35:55', 'used', '2025-09-11 20:35:55', '2025-09-11 20:35:55');
INSERT INTO `user_raffle_order_003` VALUES (96, 'jay', 100301, '测试活动', 100006, '584585502041', '2025-09-11 12:37:04', 'used', '2025-09-11 20:37:03', '2025-09-11 20:37:04');
INSERT INTO `user_raffle_order_003` VALUES (97, 'jay', 100301, '测试活动', 100006, '325253046571', '2025-09-11 12:37:04', 'used', '2025-09-11 20:37:03', '2025-09-11 20:37:04');
INSERT INTO `user_raffle_order_003` VALUES (98, 'jay', 100301, '测试活动', 100006, '516850082813', '2025-09-11 12:37:04', 'used', '2025-09-11 20:37:03', '2025-09-11 20:37:04');
INSERT INTO `user_raffle_order_003` VALUES (99, 'jay', 100301, '测试活动', 100006, '034783208182', '2025-09-11 12:37:05', 'used', '2025-09-11 20:37:04', '2025-09-11 20:37:05');
INSERT INTO `user_raffle_order_003` VALUES (100, 'jay', 100301, '测试活动', 100006, '619643666484', '2025-09-11 12:37:05', 'used', '2025-09-11 20:37:05', '2025-09-11 20:37:05');
INSERT INTO `user_raffle_order_003` VALUES (101, 'jay', 100301, '测试活动', 100006, '023739472554', '2025-09-11 12:37:05', 'used', '2025-09-11 20:37:05', '2025-09-11 20:37:05');
INSERT INTO `user_raffle_order_003` VALUES (102, 'jay', 100301, '测试活动', 100006, '469960539768', '2025-09-11 12:37:05', 'used', '2025-09-11 20:37:05', '2025-09-11 20:37:05');
INSERT INTO `user_raffle_order_003` VALUES (103, 'jay', 100301, '测试活动', 100006, '721474107557', '2025-09-11 12:37:05', 'used', '2025-09-11 20:37:05', '2025-09-11 20:37:05');
INSERT INTO `user_raffle_order_003` VALUES (104, 'jay', 100301, '测试活动', 100006, '510549235023', '2025-09-11 12:37:05', 'used', '2025-09-11 20:37:05', '2025-09-11 20:37:06');
INSERT INTO `user_raffle_order_003` VALUES (105, 'jay', 100301, '测试活动', 100006, '693555010533', '2025-09-11 12:37:05', 'used', '2025-09-11 20:37:05', '2025-09-11 20:37:06');
INSERT INTO `user_raffle_order_003` VALUES (106, 'l7nrqi39', 100301, '测试活动', 100006, '001082865194', '2025-09-11 12:37:07', 'used', '2025-09-11 20:37:06', '2025-09-11 20:37:07');
INSERT INTO `user_raffle_order_003` VALUES (107, 'jay', 100301, '测试活动', 100006, '870993828346', '2025-09-11 12:40:05', 'used', '2025-09-11 20:40:04', '2025-09-11 20:40:05');
INSERT INTO `user_raffle_order_003` VALUES (108, 'jay', 100301, '测试活动', 100006, '298068288177', '2025-09-11 12:40:32', 'used', '2025-09-11 20:40:32', '2025-09-11 20:40:32');
INSERT INTO `user_raffle_order_003` VALUES (109, 'l7nrqi39', 100301, '测试活动', 100006, '703591723821', '2025-09-11 12:40:35', 'used', '2025-09-11 20:40:35', '2025-09-11 20:40:35');
INSERT INTO `user_raffle_order_003` VALUES (110, 'lh7lnf.iul17', 100301, '测试活动', 100006, '756798941877', '2025-09-11 12:40:37', 'used', '2025-09-11 20:40:37', '2025-09-11 20:40:37');
INSERT INTO `user_raffle_order_003` VALUES (111, 'ggqryd_skd44', 100301, '测试活动', 100006, '202461115156', '2025-09-11 12:40:42', 'used', '2025-09-11 20:40:42', '2025-09-11 20:40:42');
INSERT INTO `user_raffle_order_003` VALUES (112, 'lm1_p1k', 100301, '测试活动', 100006, '223908492570', '2025-09-11 12:40:45', 'used', '2025-09-11 20:40:45', '2025-09-11 20:40:45');
INSERT INTO `user_raffle_order_003` VALUES (113, 'pgknz9.q5795', 100301, '测试活动', 100006, '084473582561', '2025-09-11 12:40:47', 'used', '2025-09-11 20:40:47', '2025-09-11 20:40:47');
INSERT INTO `user_raffle_order_003` VALUES (114, 'gs2m5650', 100301, '测试活动', 100006, '819518130540', '2025-09-11 12:40:48', 'used', '2025-09-11 20:40:48', '2025-09-11 20:40:48');
INSERT INTO `user_raffle_order_003` VALUES (115, 'lm1i2v83', 100301, '测试活动', 100006, '618271231279', '2025-09-11 12:40:53', 'used', '2025-09-11 20:40:53', '2025-09-11 20:40:53');
INSERT INTO `user_raffle_order_003` VALUES (116, 'tt6kfy43', 100301, '测试活动', 100006, '617818152639', '2025-09-11 12:40:56', 'used', '2025-09-11 20:40:56', '2025-09-11 20:40:56');
INSERT INTO `user_raffle_order_003` VALUES (117, 'i2xmy2_fj6', 100301, '测试活动', 100006, '729341864240', '2025-09-11 12:41:02', 'used', '2025-09-11 20:41:02', '2025-09-11 20:41:02');
INSERT INTO `user_raffle_order_003` VALUES (118, 'izhpd6_jwt19', 100301, '测试活动', 100006, '514193364784', '2025-09-11 12:41:03', 'used', '2025-09-11 20:41:03', '2025-09-11 20:41:03');
INSERT INTO `user_raffle_order_003` VALUES (119, 'mawfmn82', 100301, '测试活动', 100006, '892744627134', '2025-09-11 12:41:06', 'used', '2025-09-11 20:41:06', '2025-09-11 20:41:06');
INSERT INTO `user_raffle_order_003` VALUES (120, 'fmnrmi_ff1', 100301, '测试活动', 100006, '881604419574', '2025-09-11 12:41:07', 'used', '2025-09-11 20:41:07', '2025-09-11 20:41:07');
INSERT INTO `user_raffle_order_003` VALUES (121, 'v9rlnf.gbl49', 100301, '测试活动', 100006, '695698496764', '2025-09-11 12:41:09', 'used', '2025-09-11 20:41:09', '2025-09-11 20:41:09');
INSERT INTO `user_raffle_order_003` VALUES (122, 'v9ru724', 100301, '测试活动', 100006, '789400922936', '2025-09-11 12:41:14', 'used', '2025-09-11 20:41:13', '2025-09-11 20:41:14');
INSERT INTO `user_raffle_order_003` VALUES (123, 'izhlq4.lpvfim58', 100301, '测试活动', 100006, '907577096403', '2025-09-11 12:41:19', 'used', '2025-09-11 20:41:18', '2025-09-11 20:41:19');
INSERT INTO `user_raffle_order_003` VALUES (124, 'mawtue37', 100301, '测试活动', 100006, '892261763789', '2025-09-11 12:41:22', 'used', '2025-09-11 20:41:21', '2025-09-11 20:41:22');
INSERT INTO `user_raffle_order_003` VALUES (125, 'jwupu524', 100301, '测试活动', 100006, '249155116559', '2025-09-11 12:41:23', 'used', '2025-09-11 20:41:22', '2025-09-11 20:41:23');
INSERT INTO `user_raffle_order_003` VALUES (126, 'tbfnuy58', 100301, '测试活动', 100006, '488087498598', '2025-09-11 12:41:28', 'used', '2025-09-11 20:41:27', '2025-09-11 20:41:27');
INSERT INTO `user_raffle_order_003` VALUES (127, 'ffxlmf_l9v84', 100301, '测试活动', 100006, '882275162643', '2025-09-11 12:41:28', 'used', '2025-09-11 20:41:28', '2025-09-11 20:41:28');
INSERT INTO `user_raffle_order_003` VALUES (128, 'irescp12', 100301, '测试活动', 100006, '495205823805', '2025-09-11 12:41:31', 'used', '2025-09-11 20:41:30', '2025-09-11 20:41:31');
INSERT INTO `user_raffle_order_003` VALUES (129, 'mawfmn_rlk', 100301, '测试活动', 100006, '633936992398', '2025-09-11 12:41:36', 'used', '2025-09-11 20:41:36', '2025-09-11 20:41:36');
INSERT INTO `user_raffle_order_003` VALUES (130, 'izhpd6_lxi', 100301, '测试活动', 100006, '830180589722', '2025-09-11 12:41:39', 'used', '2025-09-11 20:41:38', '2025-09-11 20:41:39');
INSERT INTO `user_raffle_order_003` VALUES (131, 'v9rmdm_gp82', 100301, '测试活动', 100006, '160882356236', '2025-09-11 12:41:43', 'used', '2025-09-11 20:41:43', '2025-09-11 20:41:43');
INSERT INTO `user_raffle_order_003` VALUES (132, 'kutqrx_lal54', 100301, '测试活动', 100006, '478453058390', '2025-09-11 12:41:49', 'used', '2025-09-11 20:41:49', '2025-09-11 20:41:49');
INSERT INTO `user_raffle_order_003` VALUES (133, 'rmftt4.k28', 100301, '测试活动', 100006, '536016673960', '2025-09-11 12:41:51', 'used', '2025-09-11 20:41:50', '2025-09-11 20:41:51');
INSERT INTO `user_raffle_order_003` VALUES (134, 'k8qscp97', 100301, '测试活动', 100006, '705400855619', '2025-09-11 12:41:52', 'used', '2025-09-11 20:41:52', '2025-09-11 20:41:52');
INSERT INTO `user_raffle_order_003` VALUES (135, 'ixjlj1_kk17', 100301, '测试活动', 100006, '301912636493', '2025-09-11 12:41:53', 'used', '2025-09-11 20:41:53', '2025-09-11 20:41:53');
INSERT INTO `user_raffle_order_003` VALUES (136, 'p72k3d.kjv', 100301, '测试活动', 100006, '048535936908', '2025-09-11 12:41:55', 'used', '2025-09-11 20:41:55', '2025-09-11 20:41:55');
INSERT INTO `user_raffle_order_003` VALUES (137, 'ttbtw9_lr4', 100301, '测试活动', 100006, '102266291534', '2025-09-11 12:42:06', 'used', '2025-09-11 20:42:06', '2025-09-11 20:42:06');
INSERT INTO `user_raffle_order_003` VALUES (138, 'ffxlmf_fk177', 100301, '测试活动', 100006, '702310873139', '2025-09-11 12:42:11', 'used', '2025-09-11 20:42:11', '2025-09-11 20:42:11');
INSERT INTO `user_raffle_order_003` VALUES (139, 'v9rlnf_iwn40', 100301, '测试活动', 100006, '946574004672', '2025-09-11 12:42:16', 'used', '2025-09-11 20:42:15', '2025-09-11 20:42:16');
INSERT INTO `user_raffle_order_003` VALUES (140, 'k5qkfk_tj383', 100301, '测试活动', 100006, '001363804835', '2025-09-11 12:42:17', 'used', '2025-09-11 20:42:16', '2025-09-11 20:42:16');
INSERT INTO `user_raffle_order_003` VALUES (141, 'jcvsbt_rx7', 100301, '测试活动', 100006, '611789078193', '2025-09-11 12:42:29', 'used', '2025-09-11 20:42:29', '2025-09-11 20:42:29');
INSERT INTO `user_raffle_order_003` VALUES (142, 'ui839', 100301, '测试活动', 100006, '198687250750', '2025-09-11 12:42:32', 'used', '2025-09-11 20:42:32', '2025-09-11 20:42:32');
INSERT INTO `user_raffle_order_003` VALUES (143, 'k5qscp59', 100301, '测试活动', 100006, '133167573381', '2025-09-11 12:42:35', 'used', '2025-09-11 20:42:34', '2025-09-11 20:42:35');
INSERT INTO `user_raffle_order_003` VALUES (144, 'ppm.nz1', 100301, '测试活动', 100006, '398530093132', '2025-09-11 12:42:39', 'used', '2025-09-11 20:42:38', '2025-09-11 20:42:39');
INSERT INTO `user_raffle_order_003` VALUES (145, 'i1cvdk65', 100301, '测试活动', 100006, '837323220757', '2025-09-11 12:42:40', 'used', '2025-09-11 20:42:39', '2025-09-11 20:42:39');
INSERT INTO `user_raffle_order_003` VALUES (146, 'fmnnzu.keb98', 100301, '测试活动', 100006, '345776518808', '2025-09-11 12:42:46', 'used', '2025-09-11 20:42:45', '2025-09-11 20:42:46');
INSERT INTO `user_raffle_order_003` VALUES (147, 'negtue_psi98', 100301, '测试活动', 100006, '089682630429', '2025-09-11 12:42:57', 'used', '2025-09-11 20:42:56', '2025-09-11 20:42:56');
INSERT INTO `user_raffle_order_003` VALUES (148, 'mawnuy_hce', 100301, '测试活动', 100006, '493552799216', '2025-09-11 12:43:01', 'used', '2025-09-11 20:43:01', '2025-09-11 20:43:01');
INSERT INTO `user_raffle_order_003` VALUES (149, 'lmfmhh_kd7', 100301, '测试活动', 100006, '868485637198', '2025-09-11 12:43:09', 'used', '2025-09-11 20:43:09', '2025-09-11 20:43:09');
INSERT INTO `user_raffle_order_003` VALUES (150, 'ggqk1z.fj980', 100301, '测试活动', 100006, '581537762946', '2025-09-11 12:43:16', 'used', '2025-09-11 20:43:16', '2025-09-11 20:43:16');
INSERT INTO `user_raffle_order_003` VALUES (151, 'ryilj19', 100301, '测试活动', 100006, '761018521456', '2025-09-11 12:43:17', 'used', '2025-09-11 20:43:16', '2025-09-11 20:43:17');
INSERT INTO `user_raffle_order_003` VALUES (152, 'ggqk1z31', 100301, '测试活动', 100006, '273433046821', '2025-09-11 12:43:20', 'used', '2025-09-11 20:43:20', '2025-09-11 20:43:20');
INSERT INTO `user_raffle_order_003` VALUES (153, 'lh7lnf.g53', 100301, '测试活动', 100006, '218012945362', '2025-09-11 12:43:22', 'used', '2025-09-11 20:43:21', '2025-09-11 20:43:22');
INSERT INTO `user_raffle_order_003` VALUES (154, 'mawfmn57', 100301, '测试活动', 100006, '453407879495', '2025-09-11 12:43:23', 'used', '2025-09-11 20:43:23', '2025-09-11 20:43:23');
INSERT INTO `user_raffle_order_003` VALUES (155, 'i8lv9r.jdb', 100301, '测试活动', 100006, '792496894843', '2025-09-11 12:43:25', 'used', '2025-09-11 20:43:25', '2025-09-11 20:43:25');
INSERT INTO `user_raffle_order_003` VALUES (156, 'ts5hvb.q7z14', 100301, '测试活动', 100006, '846782563149', '2025-09-11 12:43:27', 'used', '2025-09-11 20:43:26', '2025-09-11 20:43:27');
INSERT INTO `user_raffle_order_003` VALUES (157, 'jcvsbt87', 100301, '测试活动', 100006, '294551180693', '2025-09-11 12:43:30', 'used', '2025-09-11 20:43:29', '2025-09-11 20:43:30');
INSERT INTO `user_raffle_order_003` VALUES (158, 'k5mmcx74', 100301, '测试活动', 100006, '735137035944', '2025-09-11 12:43:31', 'used', '2025-09-11 20:43:31', '2025-09-11 20:43:31');
INSERT INTO `user_raffle_order_003` VALUES (159, 'fvpts4_qg435', 100301, '测试活动', 100006, '174887400116', '2025-09-11 12:43:35', 'used', '2025-09-11 20:43:35', '2025-09-11 20:43:35');
INSERT INTO `user_raffle_order_003` VALUES (160, 'i1clo5_oti', 100301, '测试活动', 100006, '336707501296', '2025-09-11 12:43:37', 'used', '2025-09-11 20:43:37', '2025-09-11 20:43:37');
INSERT INTO `user_raffle_order_003` VALUES (161, 'fmnnzu.sib', 100301, '测试活动', 100006, '598280794989', '2025-09-11 12:43:38', 'used', '2025-09-11 20:43:38', '2025-09-11 20:43:38');
INSERT INTO `user_raffle_order_003` VALUES (162, 'hm1nuy_lid', 100301, '测试活动', 100006, '394242850896', '2025-09-11 12:43:40', 'used', '2025-09-11 20:43:40', '2025-09-11 20:43:40');
INSERT INTO `user_raffle_order_003` VALUES (163, 'jlbffg18', 100301, '测试活动', 100006, '030092172986', '2025-09-11 12:43:41', 'used', '2025-09-11 20:43:41', '2025-09-11 20:43:41');
INSERT INTO `user_raffle_order_003` VALUES (164, 'pgknz911', 100301, '测试活动', 100006, '368273962486', '2025-09-11 12:43:42', 'used', '2025-09-11 20:43:41', '2025-09-11 20:43:42');
INSERT INTO `user_raffle_order_003` VALUES (165, 'k8qlqi.nel95', 100301, '测试活动', 100006, '126181316058', '2025-09-11 12:43:46', 'used', '2025-09-11 20:43:46', '2025-09-11 20:43:46');
INSERT INTO `user_raffle_order_003` VALUES (166, 'fgwuih.gkv94', 100301, '测试活动', 100006, '296798147319', '2025-09-11 12:43:52', 'used', '2025-09-11 20:43:52', '2025-09-11 20:43:52');
INSERT INTO `user_raffle_order_003` VALUES (167, 'jay', 100301, '测试活动', 100006, '425692335025', '2025-09-12 04:14:34', 'used', '2025-09-12 12:14:33', '2025-09-12 12:14:34');

SET FOREIGN_KEY_CHECKS = 1;
