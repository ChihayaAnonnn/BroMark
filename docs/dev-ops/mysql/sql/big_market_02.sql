/*
 Navicat Premium Dump SQL

 Source Server         : bm-mysql
 Source Server Type    : MySQL
 Source Server Version : 90300 (9.3.0)
 Source Host           : 172.31.74.95:13306
 Source Schema         : big_market_02

 Target Server Type    : MySQL
 Target Server Version : 90300 (9.3.0)
 File Encoding         : 65001

 Date: 12/09/2025 13:32:02
*/
create database big_market_02;
use big_market_02;
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
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_user_id_activity_id`(`user_id` ASC, `activity_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 259 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抽奖活动账户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of raffle_activity_account
-- ----------------------------
INSERT INTO `raffle_activity_account` VALUES (7, 'fgpsbt_go848', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 17:16:26', '2025-09-11 17:16:26');
INSERT INTO `raffle_activity_account` VALUES (8, 'mg0iuc15', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:15:10', '2025-09-11 19:15:10');
INSERT INTO `raffle_activity_account` VALUES (9, 'k1zggq91', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:15:10', '2025-09-11 19:15:10');
INSERT INTO `raffle_activity_account` VALUES (10, 'ppm52', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:15:10', '2025-09-11 19:15:10');
INSERT INTO `raffle_activity_account` VALUES (11, 'm8eiuk.ssq48', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:15:10', '2025-09-11 19:15:10');
INSERT INTO `raffle_activity_account` VALUES (12, 'pu9ggq31', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:15:10', '2025-09-11 19:15:10');
INSERT INTO `raffle_activity_account` VALUES (13, 'izhlq4_gii', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:15:10', '2025-09-11 19:15:10');
INSERT INTO `raffle_activity_account` VALUES (14, 'jzzgm357', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:15:10', '2025-09-11 19:15:10');
INSERT INTO `raffle_activity_account` VALUES (15, 'v9blnf.uc9', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:15:11', '2025-09-11 19:15:11');
INSERT INTO `raffle_activity_account` VALUES (16, 'kgs62', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:15:11', '2025-09-11 19:15:11');
INSERT INTO `raffle_activity_account` VALUES (17, 'o97fmn_q0b37', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:08', '2025-09-11 19:16:08');
INSERT INTO `raffle_activity_account` VALUES (18, 'gztmft40', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:08', '2025-09-11 19:16:08');
INSERT INTO `raffle_activity_account` VALUES (19, 'izhlq4.rxn', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:09', '2025-09-11 19:16:09');
INSERT INTO `raffle_activity_account` VALUES (20, 'tbfnuy.vgw', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:09', '2025-09-11 19:16:09');
INSERT INTO `raffle_activity_account` VALUES (21, 'o97kgs32', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:09', '2025-09-11 19:16:09');
INSERT INTO `raffle_activity_account` VALUES (22, 'ggqlnf_tca', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:09', '2025-09-11 19:16:09');
INSERT INTO `raffle_activity_account` VALUES (23, 'jlbffg.tu527', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:09', '2025-09-11 19:16:09');
INSERT INTO `raffle_activity_account` VALUES (24, 'izhlu897', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:09', '2025-09-11 19:16:09');
INSERT INTO `raffle_activity_account` VALUES (25, 'i2xmy2.r8j', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:10', '2025-09-11 19:16:10');
INSERT INTO `raffle_activity_account` VALUES (26, 'ggqk1z.ijv31', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:10', '2025-09-11 19:16:10');
INSERT INTO `raffle_activity_account` VALUES (27, 'freg6n24', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:10', '2025-09-11 19:16:10');
INSERT INTO `raffle_activity_account` VALUES (28, 'k8qi488', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:10', '2025-09-11 19:16:10');
INSERT INTO `raffle_activity_account` VALUES (29, 'ryiiuk88', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:10', '2025-09-11 19:16:10');
INSERT INTO `raffle_activity_account` VALUES (30, 'lm1sbt_seo', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:10', '2025-09-11 19:16:10');
INSERT INTO `raffle_activity_account` VALUES (31, 'me4nzu4', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:11', '2025-09-11 19:16:11');
INSERT INTO `raffle_activity_account` VALUES (32, 'k8qscp8', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:11', '2025-09-11 19:16:11');
INSERT INTO `raffle_activity_account` VALUES (33, 'o97sbt73', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:11', '2025-09-11 19:16:11');
INSERT INTO `raffle_activity_account` VALUES (34, 'izh38', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:11', '2025-09-11 19:16:11');
INSERT INTO `raffle_activity_account` VALUES (35, 'lm1sds.fevfrz', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:11', '2025-09-11 19:16:11');
INSERT INTO `raffle_activity_account` VALUES (36, 'lmfmhh36', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:12', '2025-09-11 19:16:12');
INSERT INTO `raffle_activity_account` VALUES (37, 'm97k5q34', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:12', '2025-09-11 19:16:12');
INSERT INTO `raffle_activity_account` VALUES (38, 'fvpki783', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:12', '2025-09-11 19:16:12');
INSERT INTO `raffle_activity_account` VALUES (39, 'feog2w_pet4', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:12', '2025-09-11 19:16:12');
INSERT INTO `raffle_activity_account` VALUES (40, 'pa8kfk.gk458', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:12', '2025-09-11 19:16:12');
INSERT INTO `raffle_activity_account` VALUES (41, 'tbfnuy_gcf', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:12', '2025-09-11 19:16:12');
INSERT INTO `raffle_activity_account` VALUES (42, 'k1zggq8', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:12', '2025-09-11 19:16:12');
INSERT INTO `raffle_activity_account` VALUES (43, 'tbfnuy.o8678', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:12', '2025-09-11 19:16:12');
INSERT INTO `raffle_activity_account` VALUES (44, 'i1cl5v_oa71', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:13', '2025-09-11 19:16:13');
INSERT INTO `raffle_activity_account` VALUES (45, 'k18iui33', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:13', '2025-09-11 19:16:13');
INSERT INTO `raffle_activity_account` VALUES (46, 'freg6n.pu571', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:13', '2025-09-11 19:16:13');
INSERT INTO `raffle_activity_account` VALUES (47, 'ucop8d_skb26', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:13', '2025-09-11 19:16:13');
INSERT INTO `raffle_activity_account` VALUES (48, 'ts5hvb.oti', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:13', '2025-09-11 19:16:13');
INSERT INTO `raffle_activity_account` VALUES (49, 'k88to3_fj9', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:13', '2025-09-11 19:16:13');
INSERT INTO `raffle_activity_account` VALUES (50, 'jzzgm3_kls', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:13', '2025-09-11 19:16:13');
INSERT INTO `raffle_activity_account` VALUES (51, 'ui8_noz35', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:14', '2025-09-11 19:16:14');
INSERT INTO `raffle_activity_account` VALUES (52, 'hm1p8q25', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:14', '2025-09-11 19:16:14');
INSERT INTO `raffle_activity_account` VALUES (53, 'izhsf0.nky82', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:14', '2025-09-11 19:16:14');
INSERT INTO `raffle_activity_account` VALUES (54, 'v9ru72.px5', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:14', '2025-09-11 19:16:14');
INSERT INTO `raffle_activity_account` VALUES (55, 'lmfmhh_k28', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:14', '2025-09-11 19:16:14');
INSERT INTO `raffle_activity_account` VALUES (56, 'ts5hvb63', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:14', '2025-09-11 19:16:14');
INSERT INTO `raffle_activity_account` VALUES (57, 'vacsbt_fse16', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:14', '2025-09-11 19:16:14');
INSERT INTO `raffle_activity_account` VALUES (58, 'jwui2v_g1y56', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:14', '2025-09-11 19:16:14');
INSERT INTO `raffle_activity_account` VALUES (59, 'n0emzg_q0b61', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:15', '2025-09-11 19:16:15');
INSERT INTO `raffle_activity_account` VALUES (60, 'ixjit6_tom', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:15', '2025-09-11 19:16:15');
INSERT INTO `raffle_activity_account` VALUES (61, 'fevfoz_pca37', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:15', '2025-09-11 19:16:15');
INSERT INTO `raffle_activity_account` VALUES (62, 'lh7k1z_go8', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:15', '2025-09-11 19:16:15');
INSERT INTO `raffle_activity_account` VALUES (63, 'mawtue.tfa', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:15', '2025-09-11 19:16:15');
INSERT INTO `raffle_activity_account` VALUES (64, 'k8qi48_lid81', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:15', '2025-09-11 19:16:15');
INSERT INTO `raffle_activity_account` VALUES (65, 'mawgge40', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:16', '2025-09-11 19:16:16');
INSERT INTO `raffle_activity_account` VALUES (66, 'n0emzg_kfz25', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:16', '2025-09-11 19:16:16');
INSERT INTO `raffle_activity_account` VALUES (67, 'ob5j6i_g7u34', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:16', '2025-09-11 19:16:16');
INSERT INTO `raffle_activity_account` VALUES (68, 'fmni483', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:16', '2025-09-11 19:16:16');
INSERT INTO `raffle_activity_account` VALUES (69, 'v9bmdm_u0617', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:16', '2025-09-11 19:16:16');
INSERT INTO `raffle_activity_account` VALUES (70, 'gncsbt31', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:16', '2025-09-11 19:16:16');
INSERT INTO `raffle_activity_account` VALUES (71, 'lh7lnf81', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:16', '2025-09-11 19:16:16');
INSERT INTO `raffle_activity_account` VALUES (72, 'ixjit6.ldu80', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:17', '2025-09-11 19:16:17');
INSERT INTO `raffle_activity_account` VALUES (73, 'k5qscp66', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:18', '2025-09-11 19:16:18');
INSERT INTO `raffle_activity_account` VALUES (74, 'lm1sbt53', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:18', '2025-09-11 19:16:18');
INSERT INTO `raffle_activity_account` VALUES (75, 'i1clo591', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:18', '2025-09-11 19:16:18');
INSERT INTO `raffle_activity_account` VALUES (76, 'ryilj176', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:18', '2025-09-11 19:16:18');
INSERT INTO `raffle_activity_account` VALUES (77, 'i1cvdk30', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:18', '2025-09-11 19:16:18');
INSERT INTO `raffle_activity_account` VALUES (78, 'irescp_lef33', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:18', '2025-09-11 19:16:18');
INSERT INTO `raffle_activity_account` VALUES (79, 'i1csbt_n5f', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:18', '2025-09-11 19:16:18');
INSERT INTO `raffle_activity_account` VALUES (80, 'ggqryd.rwe1', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:19', '2025-09-11 19:16:19');
INSERT INTO `raffle_activity_account` VALUES (81, 'lh7lnf87', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:19', '2025-09-11 19:16:19');
INSERT INTO `raffle_activity_account` VALUES (82, 'oq3kgn_i6780', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:19', '2025-09-11 19:16:19');
INSERT INTO `raffle_activity_account` VALUES (83, 'o97scp.gp8', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:19', '2025-09-11 19:16:19');
INSERT INTO `raffle_activity_account` VALUES (84, 'i1cuji.kdw21', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:19', '2025-09-11 19:16:19');
INSERT INTO `raffle_activity_account` VALUES (85, 'freui869', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:19', '2025-09-11 19:16:19');
INSERT INTO `raffle_activity_account` VALUES (86, 'kgs.i60', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:19', '2025-09-11 19:16:19');
INSERT INTO `raffle_activity_account` VALUES (87, 'k7znlr46', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:20', '2025-09-11 19:16:20');
INSERT INTO `raffle_activity_account` VALUES (88, 'fextfv_fip68', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:20', '2025-09-11 19:16:20');
INSERT INTO `raffle_activity_account` VALUES (89, 'p1pvgw.fkp22', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:20', '2025-09-11 19:16:20');
INSERT INTO `raffle_activity_account` VALUES (90, 'i1csbt70', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:20', '2025-09-11 19:16:20');
INSERT INTO `raffle_activity_account` VALUES (91, 'ryiiuk.ml733', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:20', '2025-09-11 19:16:20');
INSERT INTO `raffle_activity_account` VALUES (92, 'irescp.fo890', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:20', '2025-09-11 19:16:20');
INSERT INTO `raffle_activity_account` VALUES (93, 'mzitue_jql', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:20', '2025-09-11 19:16:20');
INSERT INTO `raffle_activity_account` VALUES (94, 'l7nrqi_hrg60', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:20', '2025-09-11 19:16:20');
INSERT INTO `raffle_activity_account` VALUES (95, 'tgmo3v.lpi', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:20', '2025-09-11 19:16:20');
INSERT INTO `raffle_activity_account` VALUES (96, 'lh7lnf.klt77', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:21', '2025-09-11 19:16:21');
INSERT INTO `raffle_activity_account` VALUES (97, 'jwui2v_jjl', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:21', '2025-09-11 19:16:21');
INSERT INTO `raffle_activity_account` VALUES (98, 'p72k3d96', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:21', '2025-09-11 19:16:21');
INSERT INTO `raffle_activity_account` VALUES (99, 'ts5tbf_rvh19', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:21', '2025-09-11 19:16:21');
INSERT INTO `raffle_activity_account` VALUES (100, 'lm1i2v6', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:21', '2025-09-11 19:16:21');
INSERT INTO `raffle_activity_account` VALUES (101, 'ryiiuk.mlb49', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:21', '2025-09-11 19:16:21');
INSERT INTO `raffle_activity_account` VALUES (102, 'tgmo3v_jd4', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:22', '2025-09-11 19:16:22');
INSERT INTO `raffle_activity_account` VALUES (103, 'i1cvdk27', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:22', '2025-09-11 19:16:22');
INSERT INTO `raffle_activity_account` VALUES (104, 'k7fv9r96', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:22', '2025-09-11 19:16:22');
INSERT INTO `raffle_activity_account` VALUES (105, 'ob5j6i22', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:22', '2025-09-11 19:16:22');
INSERT INTO `raffle_activity_account` VALUES (106, 'my2_q11', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:22', '2025-09-11 19:16:22');
INSERT INTO `raffle_activity_account` VALUES (107, 'ktzmzi69', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:22', '2025-09-11 19:16:22');
INSERT INTO `raffle_activity_account` VALUES (108, 'o97scp.gju20', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:22', '2025-09-11 19:16:22');
INSERT INTO `raffle_activity_account` VALUES (109, 'ggqlnf69', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:23', '2025-09-11 19:16:23');
INSERT INTO `raffle_activity_account` VALUES (110, 'v9bmdm3', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:23', '2025-09-11 19:16:23');
INSERT INTO `raffle_activity_account` VALUES (111, 'lm1md2_ggn63', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:23', '2025-09-11 19:16:23');
INSERT INTO `raffle_activity_account` VALUES (112, 'jwui2v10', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:23', '2025-09-11 19:16:23');
INSERT INTO `raffle_activity_account` VALUES (113, 'k1zk5m10', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:23', '2025-09-11 19:16:23');
INSERT INTO `raffle_activity_account` VALUES (114, 'ttbtw9.j2h', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:24', '2025-09-11 19:16:24');
INSERT INTO `raffle_activity_account` VALUES (115, 'tw9izl.n4v31', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:24', '2025-09-11 19:16:24');
INSERT INTO `raffle_activity_account` VALUES (116, 'pu9ggq.lfs39', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:24', '2025-09-11 19:16:24');
INSERT INTO `raffle_activity_account` VALUES (117, 'v9rmdm84', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:24', '2025-09-11 19:16:24');
INSERT INTO `raffle_activity_account` VALUES (118, 'k4ti7b6', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:24', '2025-09-11 19:16:24');
INSERT INTO `raffle_activity_account` VALUES (119, 'izhpd6_ffx11', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:24', '2025-09-11 19:16:24');
INSERT INTO `raffle_activity_account` VALUES (120, 'ttbtw9.i2vk1z', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:24', '2025-09-11 19:16:24');
INSERT INTO `raffle_activity_account` VALUES (121, 'v9ru72.kdt97', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:25', '2025-09-11 19:16:25');
INSERT INTO `raffle_activity_account` VALUES (122, 'gncsbt.lel22', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:25', '2025-09-11 19:16:25');
INSERT INTO `raffle_activity_account` VALUES (123, 'freg6n51', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:25', '2025-09-11 19:16:25');
INSERT INTO `raffle_activity_account` VALUES (124, 'po4sf01', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:25', '2025-09-11 19:16:25');
INSERT INTO `raffle_activity_account` VALUES (125, 'izhlq4.iyo', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:25', '2025-09-11 19:16:25');
INSERT INTO `raffle_activity_account` VALUES (126, 'ffxlmf_o1p59', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:25', '2025-09-11 19:16:25');
INSERT INTO `raffle_activity_account` VALUES (127, 'tbfv9r75', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:25', '2025-09-11 19:16:25');
INSERT INTO `raffle_activity_account` VALUES (128, 'nlrlqi31', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:26', '2025-09-11 19:16:26');
INSERT INTO `raffle_activity_account` VALUES (129, 'o97fmn35', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:26', '2025-09-11 19:16:26');
INSERT INTO `raffle_activity_account` VALUES (130, 'k7nggq_gc3', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:26', '2025-09-11 19:16:26');
INSERT INTO `raffle_activity_account` VALUES (131, 'ucop8d83', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:26', '2025-09-11 19:16:26');
INSERT INTO `raffle_activity_account` VALUES (132, 'pu9ggq_tw253', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:26', '2025-09-11 19:16:26');
INSERT INTO `raffle_activity_account` VALUES (133, 'i2xmy2.u6m', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:26', '2025-09-11 19:16:26');
INSERT INTO `raffle_activity_account` VALUES (134, 'i1cvdk63', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:26', '2025-09-11 19:16:26');
INSERT INTO `raffle_activity_account` VALUES (135, 'hm1nuy.gi196', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:27', '2025-09-11 19:16:27');
INSERT INTO `raffle_activity_account` VALUES (136, 'i1cl5v_fs1', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:27', '2025-09-11 19:16:27');
INSERT INTO `raffle_activity_account` VALUES (137, 'kgs.fh847', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:27', '2025-09-11 19:16:27');
INSERT INTO `raffle_activity_account` VALUES (138, 'freg6n_k1z', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:27', '2025-09-11 19:16:27');
INSERT INTO `raffle_activity_account` VALUES (139, 'p72k3d.uc9', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:27', '2025-09-11 19:16:27');
INSERT INTO `raffle_activity_account` VALUES (140, 'k1zk5m_n54', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:28', '2025-09-11 19:16:28');
INSERT INTO `raffle_activity_account` VALUES (141, 'k18to3_p0z', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:28', '2025-09-11 19:16:28');
INSERT INTO `raffle_activity_account` VALUES (142, 'tbfv9r.lpvfim97', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:28', '2025-09-11 19:16:28');
INSERT INTO `raffle_activity_account` VALUES (143, 'pu9ggq38', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:29', '2025-09-11 19:16:29');
INSERT INTO `raffle_activity_account` VALUES (144, 'k8qlqi2', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:29', '2025-09-11 19:16:29');
INSERT INTO `raffle_activity_account` VALUES (145, 'hm1i2v_kkh', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:29', '2025-09-11 19:16:29');
INSERT INTO `raffle_activity_account` VALUES (146, 'mawnuy_nh7', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:29', '2025-09-11 19:16:29');
INSERT INTO `raffle_activity_account` VALUES (147, 'vacsbt43', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:29', '2025-09-11 19:16:29');
INSERT INTO `raffle_activity_account` VALUES (148, 'o97sbt16', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:30', '2025-09-11 19:16:30');
INSERT INTO `raffle_activity_account` VALUES (149, 'k88to374', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:30', '2025-09-11 19:16:30');
INSERT INTO `raffle_activity_account` VALUES (150, 'i1cvdk.k7f66', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:30', '2025-09-11 19:16:30');
INSERT INTO `raffle_activity_account` VALUES (151, 'lmfmhh_rwe', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:30', '2025-09-11 19:16:30');
INSERT INTO `raffle_activity_account` VALUES (152, 'lmfmhh.k2j73', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:30', '2025-09-11 19:16:30');
INSERT INTO `raffle_activity_account` VALUES (153, 'ob5j6i.ik1', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:30', '2025-09-11 19:16:30');
INSERT INTO `raffle_activity_account` VALUES (154, 'hm1i2v66', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:30', '2025-09-11 19:16:30');
INSERT INTO `raffle_activity_account` VALUES (155, 'is8k1z_pvp32', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:31', '2025-09-11 19:16:31');
INSERT INTO `raffle_activity_account` VALUES (156, 'o97sbt64', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:31', '2025-09-11 19:16:31');
INSERT INTO `raffle_activity_account` VALUES (157, 'lzbmd2_u4x', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:31', '2025-09-11 19:16:31');
INSERT INTO `raffle_activity_account` VALUES (158, 'gs2k34.qx879', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:31', '2025-09-11 19:16:31');
INSERT INTO `raffle_activity_account` VALUES (159, 'p1pscp87', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:31', '2025-09-11 19:16:31');
INSERT INTO `raffle_activity_account` VALUES (160, 'nlrlqi16', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:31', '2025-09-11 19:16:31');
INSERT INTO `raffle_activity_account` VALUES (161, 'k5mit6.fke', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:31', '2025-09-11 19:16:31');
INSERT INTO `raffle_activity_account` VALUES (162, 'mawgge_uws', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:31', '2025-09-11 19:16:31');
INSERT INTO `raffle_activity_account` VALUES (163, 'i1crqi.i78', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:31', '2025-09-11 19:16:31');
INSERT INTO `raffle_activity_account` VALUES (164, 'tgmo3v.l7p33', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:31', '2025-09-11 19:16:31');
INSERT INTO `raffle_activity_account` VALUES (165, 'k7nggq23', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:31', '2025-09-11 19:16:31');
INSERT INTO `raffle_activity_account` VALUES (166, 'hm1nuy_n5c', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:32', '2025-09-11 19:16:32');
INSERT INTO `raffle_activity_account` VALUES (167, 'gs2m565', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:32', '2025-09-11 19:16:32');
INSERT INTO `raffle_activity_account` VALUES (168, 'gs2m5617', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:32', '2025-09-11 19:16:32');
INSERT INTO `raffle_activity_account` VALUES (169, 'jcvsbt.glt95', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:33', '2025-09-11 19:16:33');
INSERT INTO `raffle_activity_account` VALUES (170, 'mg0iuc_nky', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:33', '2025-09-11 19:16:33');
INSERT INTO `raffle_activity_account` VALUES (171, 'my230', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:33', '2025-09-11 19:16:33');
INSERT INTO `raffle_activity_account` VALUES (172, 'i1cvdk3', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:33', '2025-09-11 19:16:33');
INSERT INTO `raffle_activity_account` VALUES (173, 'p1pscp68', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:33', '2025-09-11 19:16:33');
INSERT INTO `raffle_activity_account` VALUES (174, 'lnf59', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:33', '2025-09-11 19:16:33');
INSERT INTO `raffle_activity_account` VALUES (175, 'ffxlmf.puf58', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:34', '2025-09-11 19:16:34');
INSERT INTO `raffle_activity_account` VALUES (176, 'k5qsbt_o3y67', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:34', '2025-09-11 19:16:34');
INSERT INTO `raffle_activity_account` VALUES (177, 'sf0ppm_gl296', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:34', '2025-09-11 19:16:34');
INSERT INTO `raffle_activity_account` VALUES (178, 'ggqryd.p8h32', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:34', '2025-09-11 19:16:34');
INSERT INTO `raffle_activity_account` VALUES (179, 'lm1sbt.tcu', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:34', '2025-09-11 19:16:34');
INSERT INTO `raffle_activity_account` VALUES (180, 'izhfoz.r5v', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:34', '2025-09-11 19:16:34');
INSERT INTO `raffle_activity_account` VALUES (181, 'tw9izl_h9612', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:34', '2025-09-11 19:16:34');
INSERT INTO `raffle_activity_account` VALUES (182, 'rmftt4.k0m', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:34', '2025-09-11 19:16:34');
INSERT INTO `raffle_activity_account` VALUES (183, 'i1clo5_ny282', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:35', '2025-09-11 19:16:35');
INSERT INTO `raffle_activity_account` VALUES (184, 'izhsf0.ka036', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:35', '2025-09-11 19:16:35');
INSERT INTO `raffle_activity_account` VALUES (185, 'feog2w_tfa', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:35', '2025-09-11 19:16:35');
INSERT INTO `raffle_activity_account` VALUES (186, 'fvpts463', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:35', '2025-09-11 19:16:35');
INSERT INTO `raffle_activity_account` VALUES (187, 'ktzmzi_h96', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:35', '2025-09-11 19:16:35');
INSERT INTO `raffle_activity_account` VALUES (188, 'kgs.ojx', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:35', '2025-09-11 19:16:35');
INSERT INTO `raffle_activity_account` VALUES (189, 'o97kgs_rw844', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:35', '2025-09-11 19:16:35');
INSERT INTO `raffle_activity_account` VALUES (190, 'fvpts4_om789', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:35', '2025-09-11 19:16:35');
INSERT INTO `raffle_activity_account` VALUES (191, 'tt6kfy.oq372', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:36', '2025-09-11 19:16:36');
INSERT INTO `raffle_activity_account` VALUES (192, 'ggqlnf_fo574', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:36', '2025-09-11 19:16:36');
INSERT INTO `raffle_activity_account` VALUES (193, 'i1clo517', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:36', '2025-09-11 19:16:36');
INSERT INTO `raffle_activity_account` VALUES (194, 'lm1sds_gii61', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:36', '2025-09-11 19:16:36');
INSERT INTO `raffle_activity_account` VALUES (195, 'pu9ggq_s3t71', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:36', '2025-09-11 19:16:36');
INSERT INTO `raffle_activity_account` VALUES (196, 'tbfv9r91', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:36', '2025-09-11 19:16:36');
INSERT INTO `raffle_activity_account` VALUES (197, 'gncsbt25', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:37', '2025-09-11 19:16:37');
INSERT INTO `raffle_activity_account` VALUES (198, 'gncsbt51', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:37', '2025-09-11 19:16:37');
INSERT INTO `raffle_activity_account` VALUES (199, 'hm1p8q.mtn', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:37', '2025-09-11 19:16:37');
INSERT INTO `raffle_activity_account` VALUES (200, 'tgmo3v.ptd0', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:37', '2025-09-11 19:16:37');
INSERT INTO `raffle_activity_account` VALUES (201, 'tbfnuy.fof31', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:37', '2025-09-11 19:16:37');
INSERT INTO `raffle_activity_account` VALUES (202, 'ob5j6i.vcr10', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:37', '2025-09-11 19:16:37');
INSERT INTO `raffle_activity_account` VALUES (203, 'jlbi46.g30i1l', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:37', '2025-09-11 19:16:37');
INSERT INTO `raffle_activity_account` VALUES (204, 'gs2k34.fgw', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:38', '2025-09-11 19:16:38');
INSERT INTO `raffle_activity_account` VALUES (205, 'k1zu0c.fgs', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:38', '2025-09-11 19:16:38');
INSERT INTO `raffle_activity_account` VALUES (206, 'v9rlnf92', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:38', '2025-09-11 19:16:38');
INSERT INTO `raffle_activity_account` VALUES (207, 'mawgge_itu', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:38', '2025-09-11 19:16:38');
INSERT INTO `raffle_activity_account` VALUES (208, 'k5qgs270', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:38', '2025-09-11 19:16:38');
INSERT INTO `raffle_activity_account` VALUES (209, 'jlbffg15', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:39', '2025-09-11 19:16:39');
INSERT INTO `raffle_activity_account` VALUES (210, 'k4ti7b21', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:39', '2025-09-11 19:16:39');
INSERT INTO `raffle_activity_account` VALUES (211, 'uclgm3.nh4', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:39', '2025-09-11 19:16:39');
INSERT INTO `raffle_activity_account` VALUES (212, 'v9ru7274', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:39', '2025-09-11 19:16:39');
INSERT INTO `raffle_activity_account` VALUES (213, 'uclgm3_hs4', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:39', '2025-09-11 19:16:39');
INSERT INTO `raffle_activity_account` VALUES (214, 'v9rmdm.ui4', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:39', '2025-09-11 19:16:39');
INSERT INTO `raffle_activity_account` VALUES (215, 'rydkjv_kjv91', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:39', '2025-09-11 19:16:39');
INSERT INTO `raffle_activity_account` VALUES (216, 'k7znlr38', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:39', '2025-09-11 19:16:39');
INSERT INTO `raffle_activity_account` VALUES (217, 'p1pscp_fo50', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:40', '2025-09-11 19:16:40');
INSERT INTO `raffle_activity_account` VALUES (218, 'irescp.lel27', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:40', '2025-09-11 19:16:40');
INSERT INTO `raffle_activity_account` VALUES (219, 'ts5tbf43', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:40', '2025-09-11 19:16:40');
INSERT INTO `raffle_activity_account` VALUES (220, 'my250', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:40', '2025-09-11 19:16:40');
INSERT INTO `raffle_activity_account` VALUES (221, 'freui8.kgq', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:40', '2025-09-11 19:16:40');
INSERT INTO `raffle_activity_account` VALUES (222, 'lm1_sny', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:41', '2025-09-11 19:16:41');
INSERT INTO `raffle_activity_account` VALUES (223, 'jlbffg.nky48', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:41', '2025-09-11 19:16:41');
INSERT INTO `raffle_activity_account` VALUES (224, 'mawfmn_tm050', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:41', '2025-09-11 19:16:41');
INSERT INTO `raffle_activity_account` VALUES (225, 'fmnrmi35', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:41', '2025-09-11 19:16:41');
INSERT INTO `raffle_activity_account` VALUES (226, 'k7znlr_go8', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:41', '2025-09-11 19:16:41');
INSERT INTO `raffle_activity_account` VALUES (227, 'k8qlqi.lld83', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:41', '2025-09-11 19:16:41');
INSERT INTO `raffle_activity_account` VALUES (228, 'k5qgs271', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:41', '2025-09-11 19:16:41');
INSERT INTO `raffle_activity_account` VALUES (229, 'k5mmd2.fg017', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:41', '2025-09-11 19:16:41');
INSERT INTO `raffle_activity_account` VALUES (230, 'mg0iuc26', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:41', '2025-09-11 19:16:41');
INSERT INTO `raffle_activity_account` VALUES (231, 'k8qkfk82', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:42', '2025-09-11 19:16:42');
INSERT INTO `raffle_activity_account` VALUES (232, 'l7nrqi26', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:42', '2025-09-11 19:16:42');
INSERT INTO `raffle_activity_account` VALUES (233, 'ttbtw9_k6731', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:42', '2025-09-11 19:16:42');
INSERT INTO `raffle_activity_account` VALUES (234, 'irescp_gl4o6y', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:42', '2025-09-11 19:16:42');
INSERT INTO `raffle_activity_account` VALUES (235, 'ob5j6i_nevn5742', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:42', '2025-09-11 19:16:42');
INSERT INTO `raffle_activity_account` VALUES (236, 'fmni4885', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:42', '2025-09-11 19:16:42');
INSERT INTO `raffle_activity_account` VALUES (237, 'ppm_g3a46', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:42', '2025-09-11 19:16:42');
INSERT INTO `raffle_activity_account` VALUES (238, 'fevfoz96', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:43', '2025-09-11 19:16:43');
INSERT INTO `raffle_activity_account` VALUES (239, 'tw6my2.roi', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:43', '2025-09-11 19:16:43');
INSERT INTO `raffle_activity_account` VALUES (240, 'v9rmdm_fevfrz12', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:43', '2025-09-11 19:16:43');
INSERT INTO `raffle_activity_account` VALUES (241, 'k7znlr48', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:43', '2025-09-11 19:16:43');
INSERT INTO `raffle_activity_account` VALUES (242, 'ggqryd.twz81', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:43', '2025-09-11 19:16:43');
INSERT INTO `raffle_activity_account` VALUES (243, 'n0emzg_tog', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:44', '2025-09-11 19:16:44');
INSERT INTO `raffle_activity_account` VALUES (244, 'o97rmi.gbf98', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:44', '2025-09-11 19:16:44');
INSERT INTO `raffle_activity_account` VALUES (245, 'pd6fvp_gky51', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:44', '2025-09-11 19:16:44');
INSERT INTO `raffle_activity_account` VALUES (246, 'p72k3d_qjm', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:44', '2025-09-11 19:16:44');
INSERT INTO `raffle_activity_account` VALUES (247, 'me4hce.sld', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:44', '2025-09-11 19:16:44');
INSERT INTO `raffle_activity_account` VALUES (248, 'fmnrmi_vcr', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:44', '2025-09-11 19:16:44');
INSERT INTO `raffle_activity_account` VALUES (249, 'tt6kfy_nyg', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:44', '2025-09-11 19:16:44');
INSERT INTO `raffle_activity_account` VALUES (250, 'fozmy2_o9x53', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:44', '2025-09-11 19:16:44');
INSERT INTO `raffle_activity_account` VALUES (251, 'pd6fvp_shb', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:45', '2025-09-11 19:16:45');
INSERT INTO `raffle_activity_account` VALUES (252, 'gs2k345', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:45', '2025-09-11 19:16:45');
INSERT INTO `raffle_activity_account` VALUES (253, 'izhlq416', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:45', '2025-09-11 19:16:45');
INSERT INTO `raffle_activity_account` VALUES (254, 'fmnlj154', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:45', '2025-09-11 19:16:45');
INSERT INTO `raffle_activity_account` VALUES (255, 'tbfv9r.ul4', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:45', '2025-09-11 19:16:45');
INSERT INTO `raffle_activity_account` VALUES (256, 'fextfv63', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:45', '2025-09-11 19:16:45');
INSERT INTO `raffle_activity_account` VALUES (257, 'lm182', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:46', '2025-09-11 19:16:46');
INSERT INTO `raffle_activity_account` VALUES (258, 'jlbffg92', 100301, 1000, 999, 1000, 999, 1000, 999, '2025-09-11 19:16:46', '2025-09-11 19:16:46');

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
) ENGINE = InnoDB AUTO_INCREMENT = 258 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抽奖活动账户表-日次数' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of raffle_activity_account_day
-- ----------------------------
INSERT INTO `raffle_activity_account_day` VALUES (1, 'xiaofuge1', 100301, '2024-05-04', 10, 9, '2024-05-04 12:51:32', '2024-05-04 12:51:32');
INSERT INTO `raffle_activity_account_day` VALUES (2, 'xiaofuge2', 100301, '2024-05-04', 20, 5, '2024-05-04 12:53:01', '2024-05-04 13:57:32');
INSERT INTO `raffle_activity_account_day` VALUES (3, 'user001', 100301, '2024-05-24', 100, 96, '2024-05-24 22:31:47', '2024-05-24 22:34:37');
INSERT INTO `raffle_activity_account_day` VALUES (4, 'user001', 100301, '2024-05-27', 100, 97, '2024-05-27 22:39:24', '2024-05-27 22:42:17');
INSERT INTO `raffle_activity_account_day` VALUES (5, 'user001', 100301, '2024-05-30', 100, 98, '2024-05-30 07:20:49', '2024-05-30 07:22:10');
INSERT INTO `raffle_activity_account_day` VALUES (6, 'fgpsbt_go848', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:55', '2025-09-11 20:43:55');
INSERT INTO `raffle_activity_account_day` VALUES (7, 'mg0iuc15', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:56', '2025-09-11 20:43:56');
INSERT INTO `raffle_activity_account_day` VALUES (8, 'k1zggq91', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:57', '2025-09-11 20:43:57');
INSERT INTO `raffle_activity_account_day` VALUES (9, 'ppm52', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:57', '2025-09-11 20:43:57');
INSERT INTO `raffle_activity_account_day` VALUES (10, 'm8eiuk.ssq48', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:58', '2025-09-11 20:43:58');
INSERT INTO `raffle_activity_account_day` VALUES (11, 'pu9ggq31', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:43:59', '2025-09-11 20:43:59');
INSERT INTO `raffle_activity_account_day` VALUES (12, 'izhlq4_gii', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:00', '2025-09-11 20:44:00');
INSERT INTO `raffle_activity_account_day` VALUES (13, 'jzzgm357', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:01', '2025-09-11 20:44:01');
INSERT INTO `raffle_activity_account_day` VALUES (14, 'v9blnf.uc9', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:01', '2025-09-11 20:44:01');
INSERT INTO `raffle_activity_account_day` VALUES (15, 'kgs62', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:02', '2025-09-11 20:44:02');
INSERT INTO `raffle_activity_account_day` VALUES (16, 'o97fmn_q0b37', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:03', '2025-09-11 20:44:03');
INSERT INTO `raffle_activity_account_day` VALUES (17, 'gztmft40', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:04', '2025-09-11 20:44:04');
INSERT INTO `raffle_activity_account_day` VALUES (18, 'izhlq4.rxn', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:05', '2025-09-11 20:44:05');
INSERT INTO `raffle_activity_account_day` VALUES (19, 'tbfnuy.vgw', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:06', '2025-09-11 20:44:06');
INSERT INTO `raffle_activity_account_day` VALUES (20, 'o97kgs32', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:06', '2025-09-11 20:44:06');
INSERT INTO `raffle_activity_account_day` VALUES (21, 'ggqlnf_tca', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:07', '2025-09-11 20:44:07');
INSERT INTO `raffle_activity_account_day` VALUES (22, 'jlbffg.tu527', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:08', '2025-09-11 20:44:08');
INSERT INTO `raffle_activity_account_day` VALUES (23, 'izhlu897', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:09', '2025-09-11 20:44:09');
INSERT INTO `raffle_activity_account_day` VALUES (24, 'i2xmy2.r8j', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:10', '2025-09-11 20:44:10');
INSERT INTO `raffle_activity_account_day` VALUES (25, 'ggqk1z.ijv31', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:11', '2025-09-11 20:44:11');
INSERT INTO `raffle_activity_account_day` VALUES (26, 'freg6n24', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:12', '2025-09-11 20:44:12');
INSERT INTO `raffle_activity_account_day` VALUES (27, 'k8qi488', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:12', '2025-09-11 20:44:12');
INSERT INTO `raffle_activity_account_day` VALUES (28, 'ryiiuk88', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:13', '2025-09-11 20:44:13');
INSERT INTO `raffle_activity_account_day` VALUES (29, 'lm1sbt_seo', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:14', '2025-09-11 20:44:14');
INSERT INTO `raffle_activity_account_day` VALUES (30, 'me4nzu4', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:15', '2025-09-11 20:44:15');
INSERT INTO `raffle_activity_account_day` VALUES (31, 'k8qscp8', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:16', '2025-09-11 20:44:16');
INSERT INTO `raffle_activity_account_day` VALUES (32, 'o97sbt73', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:17', '2025-09-11 20:44:17');
INSERT INTO `raffle_activity_account_day` VALUES (33, 'izh38', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:18', '2025-09-11 20:44:18');
INSERT INTO `raffle_activity_account_day` VALUES (34, 'lm1sds.fevfrz', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:19', '2025-09-11 20:44:19');
INSERT INTO `raffle_activity_account_day` VALUES (35, 'lmfmhh36', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:19', '2025-09-11 20:44:19');
INSERT INTO `raffle_activity_account_day` VALUES (36, 'm97k5q34', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:20', '2025-09-11 20:44:20');
INSERT INTO `raffle_activity_account_day` VALUES (37, 'fvpki783', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:21', '2025-09-11 20:44:21');
INSERT INTO `raffle_activity_account_day` VALUES (38, 'feog2w_pet4', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:22', '2025-09-11 20:44:22');
INSERT INTO `raffle_activity_account_day` VALUES (39, 'pa8kfk.gk458', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:23', '2025-09-11 20:44:23');
INSERT INTO `raffle_activity_account_day` VALUES (40, 'tbfnuy_gcf', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:24', '2025-09-11 20:44:24');
INSERT INTO `raffle_activity_account_day` VALUES (41, 'k1zggq8', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:25', '2025-09-11 20:44:25');
INSERT INTO `raffle_activity_account_day` VALUES (42, 'tbfnuy.o8678', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:26', '2025-09-11 20:44:26');
INSERT INTO `raffle_activity_account_day` VALUES (43, 'i1cl5v_oa71', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:27', '2025-09-11 20:44:27');
INSERT INTO `raffle_activity_account_day` VALUES (44, 'k18iui33', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:27', '2025-09-11 20:44:27');
INSERT INTO `raffle_activity_account_day` VALUES (45, 'freg6n.pu571', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:28', '2025-09-11 20:44:28');
INSERT INTO `raffle_activity_account_day` VALUES (46, 'ucop8d_skb26', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:29', '2025-09-11 20:44:29');
INSERT INTO `raffle_activity_account_day` VALUES (47, 'ts5hvb.oti', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:30', '2025-09-11 20:44:30');
INSERT INTO `raffle_activity_account_day` VALUES (48, 'k88to3_fj9', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:31', '2025-09-11 20:44:31');
INSERT INTO `raffle_activity_account_day` VALUES (49, 'jzzgm3_kls', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:31', '2025-09-11 20:44:31');
INSERT INTO `raffle_activity_account_day` VALUES (50, 'ui8_noz35', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:32', '2025-09-11 20:44:32');
INSERT INTO `raffle_activity_account_day` VALUES (51, 'hm1p8q25', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:33', '2025-09-11 20:44:33');
INSERT INTO `raffle_activity_account_day` VALUES (52, 'izhsf0.nky82', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:34', '2025-09-11 20:44:34');
INSERT INTO `raffle_activity_account_day` VALUES (53, 'v9ru72.px5', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:35', '2025-09-11 20:44:35');
INSERT INTO `raffle_activity_account_day` VALUES (54, 'lmfmhh_k28', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:35', '2025-09-11 20:44:35');
INSERT INTO `raffle_activity_account_day` VALUES (55, 'ts5hvb63', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:36', '2025-09-11 20:44:36');
INSERT INTO `raffle_activity_account_day` VALUES (56, 'vacsbt_fse16', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:37', '2025-09-11 20:44:37');
INSERT INTO `raffle_activity_account_day` VALUES (57, 'jwui2v_g1y56', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:38', '2025-09-11 20:44:38');
INSERT INTO `raffle_activity_account_day` VALUES (58, 'n0emzg_q0b61', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:39', '2025-09-11 20:44:39');
INSERT INTO `raffle_activity_account_day` VALUES (59, 'ixjit6_tom', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:39', '2025-09-11 20:44:39');
INSERT INTO `raffle_activity_account_day` VALUES (60, 'fevfoz_pca37', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:40', '2025-09-11 20:44:40');
INSERT INTO `raffle_activity_account_day` VALUES (61, 'lh7k1z_go8', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:41', '2025-09-11 20:44:41');
INSERT INTO `raffle_activity_account_day` VALUES (62, 'mawtue.tfa', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:42', '2025-09-11 20:44:42');
INSERT INTO `raffle_activity_account_day` VALUES (63, 'k8qi48_lid81', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:43', '2025-09-11 20:44:43');
INSERT INTO `raffle_activity_account_day` VALUES (64, 'mawgge40', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:44', '2025-09-11 20:44:44');
INSERT INTO `raffle_activity_account_day` VALUES (65, 'n0emzg_kfz25', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:44', '2025-09-11 20:44:44');
INSERT INTO `raffle_activity_account_day` VALUES (66, 'ob5j6i_g7u34', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:45', '2025-09-11 20:44:45');
INSERT INTO `raffle_activity_account_day` VALUES (67, 'fmni483', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:46', '2025-09-11 20:44:46');
INSERT INTO `raffle_activity_account_day` VALUES (68, 'v9bmdm_u0617', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:47', '2025-09-11 20:44:47');
INSERT INTO `raffle_activity_account_day` VALUES (69, 'gncsbt31', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:48', '2025-09-11 20:44:48');
INSERT INTO `raffle_activity_account_day` VALUES (70, 'lh7lnf81', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:49', '2025-09-11 20:44:49');
INSERT INTO `raffle_activity_account_day` VALUES (71, 'ixjit6.ldu80', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:49', '2025-09-11 20:44:49');
INSERT INTO `raffle_activity_account_day` VALUES (72, 'k5qscp66', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:50', '2025-09-11 20:44:50');
INSERT INTO `raffle_activity_account_day` VALUES (73, 'lm1sbt53', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:51', '2025-09-11 20:44:51');
INSERT INTO `raffle_activity_account_day` VALUES (74, 'i1clo591', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:52', '2025-09-11 20:44:52');
INSERT INTO `raffle_activity_account_day` VALUES (75, 'ryilj176', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:53', '2025-09-11 20:44:53');
INSERT INTO `raffle_activity_account_day` VALUES (76, 'i1cvdk30', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:53', '2025-09-11 20:44:53');
INSERT INTO `raffle_activity_account_day` VALUES (77, 'irescp_lef33', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:54', '2025-09-11 20:44:54');
INSERT INTO `raffle_activity_account_day` VALUES (78, 'i1csbt_n5f', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:55', '2025-09-11 20:44:55');
INSERT INTO `raffle_activity_account_day` VALUES (79, 'ggqryd.rwe1', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:56', '2025-09-11 20:44:56');
INSERT INTO `raffle_activity_account_day` VALUES (80, 'lh7lnf87', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:57', '2025-09-11 20:44:57');
INSERT INTO `raffle_activity_account_day` VALUES (81, 'oq3kgn_i6780', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:57', '2025-09-11 20:44:57');
INSERT INTO `raffle_activity_account_day` VALUES (82, 'o97scp.gp8', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:58', '2025-09-11 20:44:58');
INSERT INTO `raffle_activity_account_day` VALUES (83, 'i1cuji.kdw21', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:44:59', '2025-09-11 20:44:59');
INSERT INTO `raffle_activity_account_day` VALUES (84, 'freui869', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:00', '2025-09-11 20:45:00');
INSERT INTO `raffle_activity_account_day` VALUES (85, 'kgs.i60', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:01', '2025-09-11 20:45:01');
INSERT INTO `raffle_activity_account_day` VALUES (86, 'k7znlr46', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:01', '2025-09-11 20:45:01');
INSERT INTO `raffle_activity_account_day` VALUES (87, 'fextfv_fip68', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:02', '2025-09-11 20:45:02');
INSERT INTO `raffle_activity_account_day` VALUES (88, 'p1pvgw.fkp22', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:03', '2025-09-11 20:45:03');
INSERT INTO `raffle_activity_account_day` VALUES (89, 'i1csbt70', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:04', '2025-09-11 20:45:04');
INSERT INTO `raffle_activity_account_day` VALUES (90, 'ryiiuk.ml733', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:05', '2025-09-11 20:45:05');
INSERT INTO `raffle_activity_account_day` VALUES (91, 'irescp.fo890', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:05', '2025-09-11 20:45:05');
INSERT INTO `raffle_activity_account_day` VALUES (92, 'mzitue_jql', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:06', '2025-09-11 20:45:06');
INSERT INTO `raffle_activity_account_day` VALUES (93, 'l7nrqi_hrg60', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:07', '2025-09-11 20:45:07');
INSERT INTO `raffle_activity_account_day` VALUES (94, 'tgmo3v.lpi', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:08', '2025-09-11 20:45:08');
INSERT INTO `raffle_activity_account_day` VALUES (95, 'lh7lnf.klt77', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:09', '2025-09-11 20:45:09');
INSERT INTO `raffle_activity_account_day` VALUES (96, 'jwui2v_jjl', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:09', '2025-09-11 20:45:09');
INSERT INTO `raffle_activity_account_day` VALUES (97, 'p72k3d96', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:10', '2025-09-11 20:45:10');
INSERT INTO `raffle_activity_account_day` VALUES (98, 'ts5tbf_rvh19', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:11', '2025-09-11 20:45:11');
INSERT INTO `raffle_activity_account_day` VALUES (99, 'lm1i2v6', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:12', '2025-09-11 20:45:12');
INSERT INTO `raffle_activity_account_day` VALUES (100, 'ryiiuk.mlb49', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:12', '2025-09-11 20:45:12');
INSERT INTO `raffle_activity_account_day` VALUES (101, 'tgmo3v_jd4', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:13', '2025-09-11 20:45:13');
INSERT INTO `raffle_activity_account_day` VALUES (102, 'i1cvdk27', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:14', '2025-09-11 20:45:14');
INSERT INTO `raffle_activity_account_day` VALUES (103, 'k7fv9r96', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:15', '2025-09-11 20:45:15');
INSERT INTO `raffle_activity_account_day` VALUES (104, 'ob5j6i22', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:16', '2025-09-11 20:45:16');
INSERT INTO `raffle_activity_account_day` VALUES (105, 'my2_q11', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:17', '2025-09-11 20:45:17');
INSERT INTO `raffle_activity_account_day` VALUES (106, 'ktzmzi69', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:17', '2025-09-11 20:45:17');
INSERT INTO `raffle_activity_account_day` VALUES (107, 'o97scp.gju20', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:18', '2025-09-11 20:45:18');
INSERT INTO `raffle_activity_account_day` VALUES (108, 'ggqlnf69', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:19', '2025-09-11 20:45:19');
INSERT INTO `raffle_activity_account_day` VALUES (109, 'v9bmdm3', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:20', '2025-09-11 20:45:20');
INSERT INTO `raffle_activity_account_day` VALUES (110, 'lm1md2_ggn63', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:21', '2025-09-11 20:45:21');
INSERT INTO `raffle_activity_account_day` VALUES (111, 'jwui2v10', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:22', '2025-09-11 20:45:22');
INSERT INTO `raffle_activity_account_day` VALUES (112, 'k1zk5m10', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:22', '2025-09-11 20:45:22');
INSERT INTO `raffle_activity_account_day` VALUES (113, 'ttbtw9.j2h', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:23', '2025-09-11 20:45:23');
INSERT INTO `raffle_activity_account_day` VALUES (114, 'tw9izl.n4v31', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:24', '2025-09-11 20:45:24');
INSERT INTO `raffle_activity_account_day` VALUES (115, 'pu9ggq.lfs39', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:25', '2025-09-11 20:45:25');
INSERT INTO `raffle_activity_account_day` VALUES (116, 'v9rmdm84', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:26', '2025-09-11 20:45:26');
INSERT INTO `raffle_activity_account_day` VALUES (117, 'k4ti7b6', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:26', '2025-09-11 20:45:26');
INSERT INTO `raffle_activity_account_day` VALUES (118, 'izhpd6_ffx11', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:27', '2025-09-11 20:45:27');
INSERT INTO `raffle_activity_account_day` VALUES (119, 'ttbtw9.i2vk1z', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:28', '2025-09-11 20:45:28');
INSERT INTO `raffle_activity_account_day` VALUES (120, 'v9ru72.kdt97', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:29', '2025-09-11 20:45:29');
INSERT INTO `raffle_activity_account_day` VALUES (121, 'gncsbt.lel22', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:30', '2025-09-11 20:45:30');
INSERT INTO `raffle_activity_account_day` VALUES (122, 'freg6n51', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:31', '2025-09-11 20:45:31');
INSERT INTO `raffle_activity_account_day` VALUES (123, 'po4sf01', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:31', '2025-09-11 20:45:31');
INSERT INTO `raffle_activity_account_day` VALUES (124, 'izhlq4.iyo', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:32', '2025-09-11 20:45:32');
INSERT INTO `raffle_activity_account_day` VALUES (125, 'ffxlmf_o1p59', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:33', '2025-09-11 20:45:33');
INSERT INTO `raffle_activity_account_day` VALUES (126, 'tbfv9r75', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:34', '2025-09-11 20:45:34');
INSERT INTO `raffle_activity_account_day` VALUES (127, 'nlrlqi31', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:34', '2025-09-11 20:45:34');
INSERT INTO `raffle_activity_account_day` VALUES (128, 'o97fmn35', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:35', '2025-09-11 20:45:35');
INSERT INTO `raffle_activity_account_day` VALUES (129, 'k7nggq_gc3', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:36', '2025-09-11 20:45:36');
INSERT INTO `raffle_activity_account_day` VALUES (130, 'ucop8d83', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:37', '2025-09-11 20:45:37');
INSERT INTO `raffle_activity_account_day` VALUES (131, 'pu9ggq_tw253', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:38', '2025-09-11 20:45:38');
INSERT INTO `raffle_activity_account_day` VALUES (132, 'i2xmy2.u6m', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:39', '2025-09-11 20:45:39');
INSERT INTO `raffle_activity_account_day` VALUES (133, 'i1cvdk63', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:39', '2025-09-11 20:45:39');
INSERT INTO `raffle_activity_account_day` VALUES (134, 'hm1nuy.gi196', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:40', '2025-09-11 20:45:40');
INSERT INTO `raffle_activity_account_day` VALUES (135, 'i1cl5v_fs1', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:41', '2025-09-11 20:45:41');
INSERT INTO `raffle_activity_account_day` VALUES (136, 'kgs.fh847', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:42', '2025-09-11 20:45:42');
INSERT INTO `raffle_activity_account_day` VALUES (137, 'freg6n_k1z', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:43', '2025-09-11 20:45:43');
INSERT INTO `raffle_activity_account_day` VALUES (138, 'p72k3d.uc9', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:43', '2025-09-11 20:45:43');
INSERT INTO `raffle_activity_account_day` VALUES (139, 'k1zk5m_n54', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:44', '2025-09-11 20:45:44');
INSERT INTO `raffle_activity_account_day` VALUES (140, 'k18to3_p0z', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:45', '2025-09-11 20:45:45');
INSERT INTO `raffle_activity_account_day` VALUES (141, 'tbfv9r.lpvfim97', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:46', '2025-09-11 20:45:46');
INSERT INTO `raffle_activity_account_day` VALUES (142, 'pu9ggq38', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:46', '2025-09-11 20:45:46');
INSERT INTO `raffle_activity_account_day` VALUES (143, 'k8qlqi2', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:47', '2025-09-11 20:45:47');
INSERT INTO `raffle_activity_account_day` VALUES (144, 'hm1i2v_kkh', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:48', '2025-09-11 20:45:48');
INSERT INTO `raffle_activity_account_day` VALUES (145, 'mawnuy_nh7', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:49', '2025-09-11 20:45:49');
INSERT INTO `raffle_activity_account_day` VALUES (146, 'vacsbt43', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:50', '2025-09-11 20:45:50');
INSERT INTO `raffle_activity_account_day` VALUES (147, 'o97sbt16', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:50', '2025-09-11 20:45:50');
INSERT INTO `raffle_activity_account_day` VALUES (148, 'k88to374', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:51', '2025-09-11 20:45:51');
INSERT INTO `raffle_activity_account_day` VALUES (149, 'i1cvdk.k7f66', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:52', '2025-09-11 20:45:52');
INSERT INTO `raffle_activity_account_day` VALUES (150, 'lmfmhh_rwe', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:53', '2025-09-11 20:45:53');
INSERT INTO `raffle_activity_account_day` VALUES (151, 'lmfmhh.k2j73', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:54', '2025-09-11 20:45:54');
INSERT INTO `raffle_activity_account_day` VALUES (152, 'ob5j6i.ik1', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:54', '2025-09-11 20:45:54');
INSERT INTO `raffle_activity_account_day` VALUES (153, 'hm1i2v66', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:55', '2025-09-11 20:45:55');
INSERT INTO `raffle_activity_account_day` VALUES (154, 'is8k1z_pvp32', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:56', '2025-09-11 20:45:56');
INSERT INTO `raffle_activity_account_day` VALUES (155, 'o97sbt64', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:57', '2025-09-11 20:45:57');
INSERT INTO `raffle_activity_account_day` VALUES (156, 'lzbmd2_u4x', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:58', '2025-09-11 20:45:58');
INSERT INTO `raffle_activity_account_day` VALUES (157, 'gs2k34.qx879', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:58', '2025-09-11 20:45:58');
INSERT INTO `raffle_activity_account_day` VALUES (158, 'p1pscp87', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:45:59', '2025-09-11 20:45:59');
INSERT INTO `raffle_activity_account_day` VALUES (159, 'nlrlqi16', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:00', '2025-09-11 20:46:00');
INSERT INTO `raffle_activity_account_day` VALUES (160, 'k5mit6.fke', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:01', '2025-09-11 20:46:01');
INSERT INTO `raffle_activity_account_day` VALUES (161, 'mawgge_uws', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:02', '2025-09-11 20:46:02');
INSERT INTO `raffle_activity_account_day` VALUES (162, 'i1crqi.i78', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:02', '2025-09-11 20:46:02');
INSERT INTO `raffle_activity_account_day` VALUES (163, 'tgmo3v.l7p33', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:03', '2025-09-11 20:46:03');
INSERT INTO `raffle_activity_account_day` VALUES (164, 'k7nggq23', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:04', '2025-09-11 20:46:04');
INSERT INTO `raffle_activity_account_day` VALUES (165, 'hm1nuy_n5c', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:05', '2025-09-11 20:46:05');
INSERT INTO `raffle_activity_account_day` VALUES (166, 'gs2m565', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:06', '2025-09-11 20:46:06');
INSERT INTO `raffle_activity_account_day` VALUES (167, 'gs2m5617', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:06', '2025-09-11 20:46:06');
INSERT INTO `raffle_activity_account_day` VALUES (168, 'jcvsbt.glt95', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:07', '2025-09-11 20:46:07');
INSERT INTO `raffle_activity_account_day` VALUES (169, 'mg0iuc_nky', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:08', '2025-09-11 20:46:08');
INSERT INTO `raffle_activity_account_day` VALUES (170, 'my230', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:09', '2025-09-11 20:46:09');
INSERT INTO `raffle_activity_account_day` VALUES (171, 'i1cvdk3', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:10', '2025-09-11 20:46:10');
INSERT INTO `raffle_activity_account_day` VALUES (172, 'p1pscp68', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:11', '2025-09-11 20:46:11');
INSERT INTO `raffle_activity_account_day` VALUES (173, 'lnf59', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:12', '2025-09-11 20:46:12');
INSERT INTO `raffle_activity_account_day` VALUES (174, 'ffxlmf.puf58', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:12', '2025-09-11 20:46:12');
INSERT INTO `raffle_activity_account_day` VALUES (175, 'k5qsbt_o3y67', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:13', '2025-09-11 20:46:13');
INSERT INTO `raffle_activity_account_day` VALUES (176, 'sf0ppm_gl296', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:14', '2025-09-11 20:46:14');
INSERT INTO `raffle_activity_account_day` VALUES (177, 'ggqryd.p8h32', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:15', '2025-09-11 20:46:15');
INSERT INTO `raffle_activity_account_day` VALUES (178, 'lm1sbt.tcu', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:16', '2025-09-11 20:46:16');
INSERT INTO `raffle_activity_account_day` VALUES (179, 'izhfoz.r5v', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:17', '2025-09-11 20:46:17');
INSERT INTO `raffle_activity_account_day` VALUES (180, 'tw9izl_h9612', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:17', '2025-09-11 20:46:17');
INSERT INTO `raffle_activity_account_day` VALUES (181, 'rmftt4.k0m', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:18', '2025-09-11 20:46:18');
INSERT INTO `raffle_activity_account_day` VALUES (182, 'i1clo5_ny282', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:19', '2025-09-11 20:46:19');
INSERT INTO `raffle_activity_account_day` VALUES (183, 'izhsf0.ka036', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:20', '2025-09-11 20:46:20');
INSERT INTO `raffle_activity_account_day` VALUES (184, 'feog2w_tfa', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:21', '2025-09-11 20:46:21');
INSERT INTO `raffle_activity_account_day` VALUES (185, 'fvpts463', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:21', '2025-09-11 20:46:21');
INSERT INTO `raffle_activity_account_day` VALUES (186, 'ktzmzi_h96', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:22', '2025-09-11 20:46:22');
INSERT INTO `raffle_activity_account_day` VALUES (187, 'kgs.ojx', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:23', '2025-09-11 20:46:23');
INSERT INTO `raffle_activity_account_day` VALUES (188, 'o97kgs_rw844', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:24', '2025-09-11 20:46:24');
INSERT INTO `raffle_activity_account_day` VALUES (189, 'fvpts4_om789', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:24', '2025-09-11 20:46:24');
INSERT INTO `raffle_activity_account_day` VALUES (190, 'tt6kfy.oq372', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:25', '2025-09-11 20:46:25');
INSERT INTO `raffle_activity_account_day` VALUES (191, 'ggqlnf_fo574', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:26', '2025-09-11 20:46:26');
INSERT INTO `raffle_activity_account_day` VALUES (192, 'i1clo517', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:27', '2025-09-11 20:46:27');
INSERT INTO `raffle_activity_account_day` VALUES (193, 'lm1sds_gii61', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:28', '2025-09-11 20:46:28');
INSERT INTO `raffle_activity_account_day` VALUES (194, 'pu9ggq_s3t71', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:29', '2025-09-11 20:46:29');
INSERT INTO `raffle_activity_account_day` VALUES (195, 'tbfv9r91', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:29', '2025-09-11 20:46:29');
INSERT INTO `raffle_activity_account_day` VALUES (196, 'gncsbt25', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:30', '2025-09-11 20:46:30');
INSERT INTO `raffle_activity_account_day` VALUES (197, 'gncsbt51', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:31', '2025-09-11 20:46:31');
INSERT INTO `raffle_activity_account_day` VALUES (198, 'hm1p8q.mtn', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:32', '2025-09-11 20:46:32');
INSERT INTO `raffle_activity_account_day` VALUES (199, 'tgmo3v.ptd0', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:33', '2025-09-11 20:46:33');
INSERT INTO `raffle_activity_account_day` VALUES (200, 'tbfnuy.fof31', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:33', '2025-09-11 20:46:33');
INSERT INTO `raffle_activity_account_day` VALUES (201, 'ob5j6i.vcr10', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:34', '2025-09-11 20:46:34');
INSERT INTO `raffle_activity_account_day` VALUES (202, 'jlbi46.g30i1l', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:35', '2025-09-11 20:46:35');
INSERT INTO `raffle_activity_account_day` VALUES (203, 'gs2k34.fgw', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:36', '2025-09-11 20:46:36');
INSERT INTO `raffle_activity_account_day` VALUES (204, 'k1zu0c.fgs', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:37', '2025-09-11 20:46:37');
INSERT INTO `raffle_activity_account_day` VALUES (205, 'v9rlnf92', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:37', '2025-09-11 20:46:37');
INSERT INTO `raffle_activity_account_day` VALUES (206, 'mawgge_itu', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:38', '2025-09-11 20:46:38');
INSERT INTO `raffle_activity_account_day` VALUES (207, 'k5qgs270', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:39', '2025-09-11 20:46:39');
INSERT INTO `raffle_activity_account_day` VALUES (208, 'jlbffg15', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:40', '2025-09-11 20:46:40');
INSERT INTO `raffle_activity_account_day` VALUES (209, 'k4ti7b21', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:41', '2025-09-11 20:46:41');
INSERT INTO `raffle_activity_account_day` VALUES (210, 'uclgm3.nh4', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:42', '2025-09-11 20:46:42');
INSERT INTO `raffle_activity_account_day` VALUES (211, 'v9ru7274', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:42', '2025-09-11 20:46:42');
INSERT INTO `raffle_activity_account_day` VALUES (212, 'uclgm3_hs4', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:43', '2025-09-11 20:46:43');
INSERT INTO `raffle_activity_account_day` VALUES (213, 'v9rmdm.ui4', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:44', '2025-09-11 20:46:44');
INSERT INTO `raffle_activity_account_day` VALUES (214, 'rydkjv_kjv91', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:45', '2025-09-11 20:46:45');
INSERT INTO `raffle_activity_account_day` VALUES (215, 'k7znlr38', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:46', '2025-09-11 20:46:46');
INSERT INTO `raffle_activity_account_day` VALUES (216, 'p1pscp_fo50', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:47', '2025-09-11 20:46:47');
INSERT INTO `raffle_activity_account_day` VALUES (217, 'irescp.lel27', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:47', '2025-09-11 20:46:47');
INSERT INTO `raffle_activity_account_day` VALUES (218, 'ts5tbf43', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:48', '2025-09-11 20:46:48');
INSERT INTO `raffle_activity_account_day` VALUES (219, 'my250', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:49', '2025-09-11 20:46:49');
INSERT INTO `raffle_activity_account_day` VALUES (220, 'freui8.kgq', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:50', '2025-09-11 20:46:50');
INSERT INTO `raffle_activity_account_day` VALUES (221, 'lm1_sny', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:51', '2025-09-11 20:46:51');
INSERT INTO `raffle_activity_account_day` VALUES (222, 'jlbffg.nky48', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:51', '2025-09-11 20:46:51');
INSERT INTO `raffle_activity_account_day` VALUES (223, 'mawfmn_tm050', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:52', '2025-09-11 20:46:52');
INSERT INTO `raffle_activity_account_day` VALUES (224, 'fmnrmi35', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:53', '2025-09-11 20:46:53');
INSERT INTO `raffle_activity_account_day` VALUES (225, 'k7znlr_go8', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:55', '2025-09-11 20:46:55');
INSERT INTO `raffle_activity_account_day` VALUES (226, 'k8qlqi.lld83', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:55', '2025-09-11 20:46:55');
INSERT INTO `raffle_activity_account_day` VALUES (227, 'k5qgs271', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:56', '2025-09-11 20:46:56');
INSERT INTO `raffle_activity_account_day` VALUES (228, 'k5mmd2.fg017', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:57', '2025-09-11 20:46:57');
INSERT INTO `raffle_activity_account_day` VALUES (229, 'mg0iuc26', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:58', '2025-09-11 20:46:58');
INSERT INTO `raffle_activity_account_day` VALUES (230, 'k8qkfk82', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:59', '2025-09-11 20:46:59');
INSERT INTO `raffle_activity_account_day` VALUES (231, 'l7nrqi26', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:46:59', '2025-09-11 20:46:59');
INSERT INTO `raffle_activity_account_day` VALUES (232, 'ttbtw9_k6731', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:00', '2025-09-11 20:47:00');
INSERT INTO `raffle_activity_account_day` VALUES (233, 'irescp_gl4o6y', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:01', '2025-09-11 20:47:01');
INSERT INTO `raffle_activity_account_day` VALUES (234, 'ob5j6i_nevn5742', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:02', '2025-09-11 20:47:02');
INSERT INTO `raffle_activity_account_day` VALUES (235, 'fmni4885', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:03', '2025-09-11 20:47:03');
INSERT INTO `raffle_activity_account_day` VALUES (236, 'ppm_g3a46', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:03', '2025-09-11 20:47:03');
INSERT INTO `raffle_activity_account_day` VALUES (237, 'fevfoz96', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:04', '2025-09-11 20:47:04');
INSERT INTO `raffle_activity_account_day` VALUES (238, 'tw6my2.roi', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:05', '2025-09-11 20:47:05');
INSERT INTO `raffle_activity_account_day` VALUES (239, 'v9rmdm_fevfrz12', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:06', '2025-09-11 20:47:06');
INSERT INTO `raffle_activity_account_day` VALUES (240, 'k7znlr48', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:07', '2025-09-11 20:47:07');
INSERT INTO `raffle_activity_account_day` VALUES (241, 'ggqryd.twz81', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:07', '2025-09-11 20:47:07');
INSERT INTO `raffle_activity_account_day` VALUES (242, 'n0emzg_tog', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:08', '2025-09-11 20:47:08');
INSERT INTO `raffle_activity_account_day` VALUES (243, 'o97rmi.gbf98', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:09', '2025-09-11 20:47:09');
INSERT INTO `raffle_activity_account_day` VALUES (244, 'pd6fvp_gky51', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:10', '2025-09-11 20:47:10');
INSERT INTO `raffle_activity_account_day` VALUES (245, 'p72k3d_qjm', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:11', '2025-09-11 20:47:11');
INSERT INTO `raffle_activity_account_day` VALUES (246, 'me4hce.sld', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:11', '2025-09-11 20:47:11');
INSERT INTO `raffle_activity_account_day` VALUES (247, 'fmnrmi_vcr', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:12', '2025-09-11 20:47:12');
INSERT INTO `raffle_activity_account_day` VALUES (248, 'tt6kfy_nyg', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:13', '2025-09-11 20:47:13');
INSERT INTO `raffle_activity_account_day` VALUES (249, 'fozmy2_o9x53', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:14', '2025-09-11 20:47:14');
INSERT INTO `raffle_activity_account_day` VALUES (250, 'pd6fvp_shb', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:15', '2025-09-11 20:47:15');
INSERT INTO `raffle_activity_account_day` VALUES (251, 'gs2k345', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:15', '2025-09-11 20:47:15');
INSERT INTO `raffle_activity_account_day` VALUES (252, 'izhlq416', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:16', '2025-09-11 20:47:16');
INSERT INTO `raffle_activity_account_day` VALUES (253, 'fmnlj154', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:17', '2025-09-11 20:47:17');
INSERT INTO `raffle_activity_account_day` VALUES (254, 'tbfv9r.ul4', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:18', '2025-09-11 20:47:18');
INSERT INTO `raffle_activity_account_day` VALUES (255, 'fextfv63', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:19', '2025-09-11 20:47:19');
INSERT INTO `raffle_activity_account_day` VALUES (256, 'lm182', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:19', '2025-09-11 20:47:19');
INSERT INTO `raffle_activity_account_day` VALUES (257, 'jlbffg92', 100301, '2025-09-11', 1000, 999, '2025-09-11 20:47:20', '2025-09-11 20:47:20');

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
) ENGINE = InnoDB AUTO_INCREMENT = 256 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抽奖活动账户表-月次数' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of raffle_activity_account_month
-- ----------------------------
INSERT INTO `raffle_activity_account_month` VALUES (1, 'xiaofuge1', 100301, '2024-05', 10, 9, '2024-05-04 12:51:32', '2024-05-04 12:51:32');
INSERT INTO `raffle_activity_account_month` VALUES (2, 'xiaofuge2', 100301, '2024-05', 20, 5, '2024-05-04 12:53:01', '2024-05-04 13:57:32');
INSERT INTO `raffle_activity_account_month` VALUES (3, 'user001', 100301, '2024-05', 100, 91, '2024-05-24 22:31:47', '2024-05-30 07:22:10');
INSERT INTO `raffle_activity_account_month` VALUES (4, 'fgpsbt_go848', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:55', '2025-09-11 20:43:55');
INSERT INTO `raffle_activity_account_month` VALUES (5, 'mg0iuc15', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:56', '2025-09-11 20:43:56');
INSERT INTO `raffle_activity_account_month` VALUES (6, 'k1zggq91', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:57', '2025-09-11 20:43:57');
INSERT INTO `raffle_activity_account_month` VALUES (7, 'ppm52', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:57', '2025-09-11 20:43:57');
INSERT INTO `raffle_activity_account_month` VALUES (8, 'm8eiuk.ssq48', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:58', '2025-09-11 20:43:58');
INSERT INTO `raffle_activity_account_month` VALUES (9, 'pu9ggq31', 100301, '2025-09', 1000, 999, '2025-09-11 20:43:59', '2025-09-11 20:43:59');
INSERT INTO `raffle_activity_account_month` VALUES (10, 'izhlq4_gii', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:00', '2025-09-11 20:44:00');
INSERT INTO `raffle_activity_account_month` VALUES (11, 'jzzgm357', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:01', '2025-09-11 20:44:01');
INSERT INTO `raffle_activity_account_month` VALUES (12, 'v9blnf.uc9', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:01', '2025-09-11 20:44:01');
INSERT INTO `raffle_activity_account_month` VALUES (13, 'kgs62', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:02', '2025-09-11 20:44:02');
INSERT INTO `raffle_activity_account_month` VALUES (14, 'o97fmn_q0b37', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:03', '2025-09-11 20:44:03');
INSERT INTO `raffle_activity_account_month` VALUES (15, 'gztmft40', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:04', '2025-09-11 20:44:04');
INSERT INTO `raffle_activity_account_month` VALUES (16, 'izhlq4.rxn', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:05', '2025-09-11 20:44:05');
INSERT INTO `raffle_activity_account_month` VALUES (17, 'tbfnuy.vgw', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:06', '2025-09-11 20:44:06');
INSERT INTO `raffle_activity_account_month` VALUES (18, 'o97kgs32', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:06', '2025-09-11 20:44:06');
INSERT INTO `raffle_activity_account_month` VALUES (19, 'ggqlnf_tca', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:07', '2025-09-11 20:44:07');
INSERT INTO `raffle_activity_account_month` VALUES (20, 'jlbffg.tu527', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:08', '2025-09-11 20:44:08');
INSERT INTO `raffle_activity_account_month` VALUES (21, 'izhlu897', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:09', '2025-09-11 20:44:09');
INSERT INTO `raffle_activity_account_month` VALUES (22, 'i2xmy2.r8j', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:10', '2025-09-11 20:44:10');
INSERT INTO `raffle_activity_account_month` VALUES (23, 'ggqk1z.ijv31', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:11', '2025-09-11 20:44:11');
INSERT INTO `raffle_activity_account_month` VALUES (24, 'freg6n24', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:12', '2025-09-11 20:44:12');
INSERT INTO `raffle_activity_account_month` VALUES (25, 'k8qi488', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:12', '2025-09-11 20:44:12');
INSERT INTO `raffle_activity_account_month` VALUES (26, 'ryiiuk88', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:13', '2025-09-11 20:44:13');
INSERT INTO `raffle_activity_account_month` VALUES (27, 'lm1sbt_seo', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:14', '2025-09-11 20:44:14');
INSERT INTO `raffle_activity_account_month` VALUES (28, 'me4nzu4', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:15', '2025-09-11 20:44:15');
INSERT INTO `raffle_activity_account_month` VALUES (29, 'k8qscp8', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:16', '2025-09-11 20:44:16');
INSERT INTO `raffle_activity_account_month` VALUES (30, 'o97sbt73', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:17', '2025-09-11 20:44:17');
INSERT INTO `raffle_activity_account_month` VALUES (31, 'izh38', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:18', '2025-09-11 20:44:18');
INSERT INTO `raffle_activity_account_month` VALUES (32, 'lm1sds.fevfrz', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:19', '2025-09-11 20:44:19');
INSERT INTO `raffle_activity_account_month` VALUES (33, 'lmfmhh36', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:19', '2025-09-11 20:44:19');
INSERT INTO `raffle_activity_account_month` VALUES (34, 'm97k5q34', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:20', '2025-09-11 20:44:20');
INSERT INTO `raffle_activity_account_month` VALUES (35, 'fvpki783', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:21', '2025-09-11 20:44:21');
INSERT INTO `raffle_activity_account_month` VALUES (36, 'feog2w_pet4', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:22', '2025-09-11 20:44:22');
INSERT INTO `raffle_activity_account_month` VALUES (37, 'pa8kfk.gk458', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:23', '2025-09-11 20:44:23');
INSERT INTO `raffle_activity_account_month` VALUES (38, 'tbfnuy_gcf', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:24', '2025-09-11 20:44:24');
INSERT INTO `raffle_activity_account_month` VALUES (39, 'k1zggq8', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:25', '2025-09-11 20:44:25');
INSERT INTO `raffle_activity_account_month` VALUES (40, 'tbfnuy.o8678', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:26', '2025-09-11 20:44:26');
INSERT INTO `raffle_activity_account_month` VALUES (41, 'i1cl5v_oa71', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:27', '2025-09-11 20:44:27');
INSERT INTO `raffle_activity_account_month` VALUES (42, 'k18iui33', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:27', '2025-09-11 20:44:27');
INSERT INTO `raffle_activity_account_month` VALUES (43, 'freg6n.pu571', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:28', '2025-09-11 20:44:28');
INSERT INTO `raffle_activity_account_month` VALUES (44, 'ucop8d_skb26', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:29', '2025-09-11 20:44:29');
INSERT INTO `raffle_activity_account_month` VALUES (45, 'ts5hvb.oti', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:30', '2025-09-11 20:44:30');
INSERT INTO `raffle_activity_account_month` VALUES (46, 'k88to3_fj9', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:31', '2025-09-11 20:44:31');
INSERT INTO `raffle_activity_account_month` VALUES (47, 'jzzgm3_kls', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:31', '2025-09-11 20:44:31');
INSERT INTO `raffle_activity_account_month` VALUES (48, 'ui8_noz35', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:32', '2025-09-11 20:44:32');
INSERT INTO `raffle_activity_account_month` VALUES (49, 'hm1p8q25', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:33', '2025-09-11 20:44:33');
INSERT INTO `raffle_activity_account_month` VALUES (50, 'izhsf0.nky82', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:34', '2025-09-11 20:44:34');
INSERT INTO `raffle_activity_account_month` VALUES (51, 'v9ru72.px5', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:35', '2025-09-11 20:44:35');
INSERT INTO `raffle_activity_account_month` VALUES (52, 'lmfmhh_k28', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:35', '2025-09-11 20:44:35');
INSERT INTO `raffle_activity_account_month` VALUES (53, 'ts5hvb63', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:36', '2025-09-11 20:44:36');
INSERT INTO `raffle_activity_account_month` VALUES (54, 'vacsbt_fse16', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:37', '2025-09-11 20:44:37');
INSERT INTO `raffle_activity_account_month` VALUES (55, 'jwui2v_g1y56', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:38', '2025-09-11 20:44:38');
INSERT INTO `raffle_activity_account_month` VALUES (56, 'n0emzg_q0b61', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:39', '2025-09-11 20:44:39');
INSERT INTO `raffle_activity_account_month` VALUES (57, 'ixjit6_tom', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:39', '2025-09-11 20:44:39');
INSERT INTO `raffle_activity_account_month` VALUES (58, 'fevfoz_pca37', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:40', '2025-09-11 20:44:40');
INSERT INTO `raffle_activity_account_month` VALUES (59, 'lh7k1z_go8', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:41', '2025-09-11 20:44:41');
INSERT INTO `raffle_activity_account_month` VALUES (60, 'mawtue.tfa', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:42', '2025-09-11 20:44:42');
INSERT INTO `raffle_activity_account_month` VALUES (61, 'k8qi48_lid81', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:43', '2025-09-11 20:44:43');
INSERT INTO `raffle_activity_account_month` VALUES (62, 'mawgge40', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:44', '2025-09-11 20:44:44');
INSERT INTO `raffle_activity_account_month` VALUES (63, 'n0emzg_kfz25', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:44', '2025-09-11 20:44:44');
INSERT INTO `raffle_activity_account_month` VALUES (64, 'ob5j6i_g7u34', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:45', '2025-09-11 20:44:45');
INSERT INTO `raffle_activity_account_month` VALUES (65, 'fmni483', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:46', '2025-09-11 20:44:46');
INSERT INTO `raffle_activity_account_month` VALUES (66, 'v9bmdm_u0617', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:47', '2025-09-11 20:44:47');
INSERT INTO `raffle_activity_account_month` VALUES (67, 'gncsbt31', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:48', '2025-09-11 20:44:48');
INSERT INTO `raffle_activity_account_month` VALUES (68, 'lh7lnf81', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:49', '2025-09-11 20:44:49');
INSERT INTO `raffle_activity_account_month` VALUES (69, 'ixjit6.ldu80', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:49', '2025-09-11 20:44:49');
INSERT INTO `raffle_activity_account_month` VALUES (70, 'k5qscp66', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:50', '2025-09-11 20:44:50');
INSERT INTO `raffle_activity_account_month` VALUES (71, 'lm1sbt53', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:51', '2025-09-11 20:44:51');
INSERT INTO `raffle_activity_account_month` VALUES (72, 'i1clo591', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:52', '2025-09-11 20:44:52');
INSERT INTO `raffle_activity_account_month` VALUES (73, 'ryilj176', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:53', '2025-09-11 20:44:53');
INSERT INTO `raffle_activity_account_month` VALUES (74, 'i1cvdk30', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:53', '2025-09-11 20:44:53');
INSERT INTO `raffle_activity_account_month` VALUES (75, 'irescp_lef33', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:54', '2025-09-11 20:44:54');
INSERT INTO `raffle_activity_account_month` VALUES (76, 'i1csbt_n5f', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:55', '2025-09-11 20:44:55');
INSERT INTO `raffle_activity_account_month` VALUES (77, 'ggqryd.rwe1', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:56', '2025-09-11 20:44:56');
INSERT INTO `raffle_activity_account_month` VALUES (78, 'lh7lnf87', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:57', '2025-09-11 20:44:57');
INSERT INTO `raffle_activity_account_month` VALUES (79, 'oq3kgn_i6780', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:57', '2025-09-11 20:44:57');
INSERT INTO `raffle_activity_account_month` VALUES (80, 'o97scp.gp8', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:58', '2025-09-11 20:44:58');
INSERT INTO `raffle_activity_account_month` VALUES (81, 'i1cuji.kdw21', 100301, '2025-09', 1000, 999, '2025-09-11 20:44:59', '2025-09-11 20:44:59');
INSERT INTO `raffle_activity_account_month` VALUES (82, 'freui869', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:00', '2025-09-11 20:45:00');
INSERT INTO `raffle_activity_account_month` VALUES (83, 'kgs.i60', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:00', '2025-09-11 20:45:00');
INSERT INTO `raffle_activity_account_month` VALUES (84, 'k7znlr46', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:01', '2025-09-11 20:45:01');
INSERT INTO `raffle_activity_account_month` VALUES (85, 'fextfv_fip68', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:02', '2025-09-11 20:45:02');
INSERT INTO `raffle_activity_account_month` VALUES (86, 'p1pvgw.fkp22', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:03', '2025-09-11 20:45:03');
INSERT INTO `raffle_activity_account_month` VALUES (87, 'i1csbt70', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:04', '2025-09-11 20:45:04');
INSERT INTO `raffle_activity_account_month` VALUES (88, 'ryiiuk.ml733', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:05', '2025-09-11 20:45:05');
INSERT INTO `raffle_activity_account_month` VALUES (89, 'irescp.fo890', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:05', '2025-09-11 20:45:05');
INSERT INTO `raffle_activity_account_month` VALUES (90, 'mzitue_jql', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:06', '2025-09-11 20:45:06');
INSERT INTO `raffle_activity_account_month` VALUES (91, 'l7nrqi_hrg60', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:07', '2025-09-11 20:45:07');
INSERT INTO `raffle_activity_account_month` VALUES (92, 'tgmo3v.lpi', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:08', '2025-09-11 20:45:08');
INSERT INTO `raffle_activity_account_month` VALUES (93, 'lh7lnf.klt77', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:09', '2025-09-11 20:45:09');
INSERT INTO `raffle_activity_account_month` VALUES (94, 'jwui2v_jjl', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:09', '2025-09-11 20:45:09');
INSERT INTO `raffle_activity_account_month` VALUES (95, 'p72k3d96', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:10', '2025-09-11 20:45:10');
INSERT INTO `raffle_activity_account_month` VALUES (96, 'ts5tbf_rvh19', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:11', '2025-09-11 20:45:11');
INSERT INTO `raffle_activity_account_month` VALUES (97, 'lm1i2v6', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:12', '2025-09-11 20:45:12');
INSERT INTO `raffle_activity_account_month` VALUES (98, 'ryiiuk.mlb49', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:12', '2025-09-11 20:45:12');
INSERT INTO `raffle_activity_account_month` VALUES (99, 'tgmo3v_jd4', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:13', '2025-09-11 20:45:13');
INSERT INTO `raffle_activity_account_month` VALUES (100, 'i1cvdk27', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:14', '2025-09-11 20:45:14');
INSERT INTO `raffle_activity_account_month` VALUES (101, 'k7fv9r96', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:15', '2025-09-11 20:45:15');
INSERT INTO `raffle_activity_account_month` VALUES (102, 'ob5j6i22', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:16', '2025-09-11 20:45:16');
INSERT INTO `raffle_activity_account_month` VALUES (103, 'my2_q11', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:17', '2025-09-11 20:45:17');
INSERT INTO `raffle_activity_account_month` VALUES (104, 'ktzmzi69', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:17', '2025-09-11 20:45:17');
INSERT INTO `raffle_activity_account_month` VALUES (105, 'o97scp.gju20', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:18', '2025-09-11 20:45:18');
INSERT INTO `raffle_activity_account_month` VALUES (106, 'ggqlnf69', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:19', '2025-09-11 20:45:19');
INSERT INTO `raffle_activity_account_month` VALUES (107, 'v9bmdm3', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:20', '2025-09-11 20:45:20');
INSERT INTO `raffle_activity_account_month` VALUES (108, 'lm1md2_ggn63', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:21', '2025-09-11 20:45:21');
INSERT INTO `raffle_activity_account_month` VALUES (109, 'jwui2v10', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:22', '2025-09-11 20:45:22');
INSERT INTO `raffle_activity_account_month` VALUES (110, 'k1zk5m10', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:22', '2025-09-11 20:45:22');
INSERT INTO `raffle_activity_account_month` VALUES (111, 'ttbtw9.j2h', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:23', '2025-09-11 20:45:23');
INSERT INTO `raffle_activity_account_month` VALUES (112, 'tw9izl.n4v31', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:24', '2025-09-11 20:45:24');
INSERT INTO `raffle_activity_account_month` VALUES (113, 'pu9ggq.lfs39', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:25', '2025-09-11 20:45:25');
INSERT INTO `raffle_activity_account_month` VALUES (114, 'v9rmdm84', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:26', '2025-09-11 20:45:26');
INSERT INTO `raffle_activity_account_month` VALUES (115, 'k4ti7b6', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:26', '2025-09-11 20:45:26');
INSERT INTO `raffle_activity_account_month` VALUES (116, 'izhpd6_ffx11', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:27', '2025-09-11 20:45:27');
INSERT INTO `raffle_activity_account_month` VALUES (117, 'ttbtw9.i2vk1z', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:28', '2025-09-11 20:45:28');
INSERT INTO `raffle_activity_account_month` VALUES (118, 'v9ru72.kdt97', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:29', '2025-09-11 20:45:29');
INSERT INTO `raffle_activity_account_month` VALUES (119, 'gncsbt.lel22', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:30', '2025-09-11 20:45:30');
INSERT INTO `raffle_activity_account_month` VALUES (120, 'freg6n51', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:30', '2025-09-11 20:45:30');
INSERT INTO `raffle_activity_account_month` VALUES (121, 'po4sf01', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:31', '2025-09-11 20:45:31');
INSERT INTO `raffle_activity_account_month` VALUES (122, 'izhlq4.iyo', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:32', '2025-09-11 20:45:32');
INSERT INTO `raffle_activity_account_month` VALUES (123, 'ffxlmf_o1p59', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:33', '2025-09-11 20:45:33');
INSERT INTO `raffle_activity_account_month` VALUES (124, 'tbfv9r75', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:34', '2025-09-11 20:45:34');
INSERT INTO `raffle_activity_account_month` VALUES (125, 'nlrlqi31', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:34', '2025-09-11 20:45:34');
INSERT INTO `raffle_activity_account_month` VALUES (126, 'o97fmn35', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:35', '2025-09-11 20:45:35');
INSERT INTO `raffle_activity_account_month` VALUES (127, 'k7nggq_gc3', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:36', '2025-09-11 20:45:36');
INSERT INTO `raffle_activity_account_month` VALUES (128, 'ucop8d83', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:37', '2025-09-11 20:45:37');
INSERT INTO `raffle_activity_account_month` VALUES (129, 'pu9ggq_tw253', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:38', '2025-09-11 20:45:38');
INSERT INTO `raffle_activity_account_month` VALUES (130, 'i2xmy2.u6m', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:38', '2025-09-11 20:45:38');
INSERT INTO `raffle_activity_account_month` VALUES (131, 'i1cvdk63', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:39', '2025-09-11 20:45:39');
INSERT INTO `raffle_activity_account_month` VALUES (132, 'hm1nuy.gi196', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:40', '2025-09-11 20:45:40');
INSERT INTO `raffle_activity_account_month` VALUES (133, 'i1cl5v_fs1', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:41', '2025-09-11 20:45:41');
INSERT INTO `raffle_activity_account_month` VALUES (134, 'kgs.fh847', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:42', '2025-09-11 20:45:42');
INSERT INTO `raffle_activity_account_month` VALUES (135, 'freg6n_k1z', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:43', '2025-09-11 20:45:43');
INSERT INTO `raffle_activity_account_month` VALUES (136, 'p72k3d.uc9', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:43', '2025-09-11 20:45:43');
INSERT INTO `raffle_activity_account_month` VALUES (137, 'k1zk5m_n54', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:44', '2025-09-11 20:45:44');
INSERT INTO `raffle_activity_account_month` VALUES (138, 'k18to3_p0z', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:45', '2025-09-11 20:45:45');
INSERT INTO `raffle_activity_account_month` VALUES (139, 'tbfv9r.lpvfim97', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:46', '2025-09-11 20:45:46');
INSERT INTO `raffle_activity_account_month` VALUES (140, 'pu9ggq38', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:46', '2025-09-11 20:45:46');
INSERT INTO `raffle_activity_account_month` VALUES (141, 'k8qlqi2', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:47', '2025-09-11 20:45:47');
INSERT INTO `raffle_activity_account_month` VALUES (142, 'hm1i2v_kkh', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:48', '2025-09-11 20:45:48');
INSERT INTO `raffle_activity_account_month` VALUES (143, 'mawnuy_nh7', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:49', '2025-09-11 20:45:49');
INSERT INTO `raffle_activity_account_month` VALUES (144, 'vacsbt43', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:50', '2025-09-11 20:45:50');
INSERT INTO `raffle_activity_account_month` VALUES (145, 'o97sbt16', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:50', '2025-09-11 20:45:50');
INSERT INTO `raffle_activity_account_month` VALUES (146, 'k88to374', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:51', '2025-09-11 20:45:51');
INSERT INTO `raffle_activity_account_month` VALUES (147, 'i1cvdk.k7f66', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:52', '2025-09-11 20:45:52');
INSERT INTO `raffle_activity_account_month` VALUES (148, 'lmfmhh_rwe', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:53', '2025-09-11 20:45:53');
INSERT INTO `raffle_activity_account_month` VALUES (149, 'lmfmhh.k2j73', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:54', '2025-09-11 20:45:54');
INSERT INTO `raffle_activity_account_month` VALUES (150, 'ob5j6i.ik1', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:54', '2025-09-11 20:45:54');
INSERT INTO `raffle_activity_account_month` VALUES (151, 'hm1i2v66', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:55', '2025-09-11 20:45:55');
INSERT INTO `raffle_activity_account_month` VALUES (152, 'is8k1z_pvp32', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:56', '2025-09-11 20:45:56');
INSERT INTO `raffle_activity_account_month` VALUES (153, 'o97sbt64', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:57', '2025-09-11 20:45:57');
INSERT INTO `raffle_activity_account_month` VALUES (154, 'lzbmd2_u4x', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:58', '2025-09-11 20:45:58');
INSERT INTO `raffle_activity_account_month` VALUES (155, 'gs2k34.qx879', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:58', '2025-09-11 20:45:58');
INSERT INTO `raffle_activity_account_month` VALUES (156, 'p1pscp87', 100301, '2025-09', 1000, 999, '2025-09-11 20:45:59', '2025-09-11 20:45:59');
INSERT INTO `raffle_activity_account_month` VALUES (157, 'nlrlqi16', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:00', '2025-09-11 20:46:00');
INSERT INTO `raffle_activity_account_month` VALUES (158, 'k5mit6.fke', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:01', '2025-09-11 20:46:01');
INSERT INTO `raffle_activity_account_month` VALUES (159, 'mawgge_uws', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:02', '2025-09-11 20:46:02');
INSERT INTO `raffle_activity_account_month` VALUES (160, 'i1crqi.i78', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:02', '2025-09-11 20:46:02');
INSERT INTO `raffle_activity_account_month` VALUES (161, 'tgmo3v.l7p33', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:03', '2025-09-11 20:46:03');
INSERT INTO `raffle_activity_account_month` VALUES (162, 'k7nggq23', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:04', '2025-09-11 20:46:04');
INSERT INTO `raffle_activity_account_month` VALUES (163, 'hm1nuy_n5c', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:05', '2025-09-11 20:46:05');
INSERT INTO `raffle_activity_account_month` VALUES (164, 'gs2m565', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:06', '2025-09-11 20:46:06');
INSERT INTO `raffle_activity_account_month` VALUES (165, 'gs2m5617', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:06', '2025-09-11 20:46:06');
INSERT INTO `raffle_activity_account_month` VALUES (166, 'jcvsbt.glt95', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:07', '2025-09-11 20:46:07');
INSERT INTO `raffle_activity_account_month` VALUES (167, 'mg0iuc_nky', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:08', '2025-09-11 20:46:08');
INSERT INTO `raffle_activity_account_month` VALUES (168, 'my230', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:09', '2025-09-11 20:46:09');
INSERT INTO `raffle_activity_account_month` VALUES (169, 'i1cvdk3', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:10', '2025-09-11 20:46:10');
INSERT INTO `raffle_activity_account_month` VALUES (170, 'p1pscp68', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:11', '2025-09-11 20:46:11');
INSERT INTO `raffle_activity_account_month` VALUES (171, 'lnf59', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:12', '2025-09-11 20:46:12');
INSERT INTO `raffle_activity_account_month` VALUES (172, 'ffxlmf.puf58', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:12', '2025-09-11 20:46:12');
INSERT INTO `raffle_activity_account_month` VALUES (173, 'k5qsbt_o3y67', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:13', '2025-09-11 20:46:13');
INSERT INTO `raffle_activity_account_month` VALUES (174, 'sf0ppm_gl296', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:14', '2025-09-11 20:46:14');
INSERT INTO `raffle_activity_account_month` VALUES (175, 'ggqryd.p8h32', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:15', '2025-09-11 20:46:15');
INSERT INTO `raffle_activity_account_month` VALUES (176, 'lm1sbt.tcu', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:16', '2025-09-11 20:46:16');
INSERT INTO `raffle_activity_account_month` VALUES (177, 'izhfoz.r5v', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:17', '2025-09-11 20:46:17');
INSERT INTO `raffle_activity_account_month` VALUES (178, 'tw9izl_h9612', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:17', '2025-09-11 20:46:17');
INSERT INTO `raffle_activity_account_month` VALUES (179, 'rmftt4.k0m', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:18', '2025-09-11 20:46:18');
INSERT INTO `raffle_activity_account_month` VALUES (180, 'i1clo5_ny282', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:19', '2025-09-11 20:46:19');
INSERT INTO `raffle_activity_account_month` VALUES (181, 'izhsf0.ka036', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:20', '2025-09-11 20:46:20');
INSERT INTO `raffle_activity_account_month` VALUES (182, 'feog2w_tfa', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:21', '2025-09-11 20:46:21');
INSERT INTO `raffle_activity_account_month` VALUES (183, 'fvpts463', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:21', '2025-09-11 20:46:21');
INSERT INTO `raffle_activity_account_month` VALUES (184, 'ktzmzi_h96', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:22', '2025-09-11 20:46:22');
INSERT INTO `raffle_activity_account_month` VALUES (185, 'kgs.ojx', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:23', '2025-09-11 20:46:23');
INSERT INTO `raffle_activity_account_month` VALUES (186, 'o97kgs_rw844', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:24', '2025-09-11 20:46:24');
INSERT INTO `raffle_activity_account_month` VALUES (187, 'fvpts4_om789', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:24', '2025-09-11 20:46:24');
INSERT INTO `raffle_activity_account_month` VALUES (188, 'tt6kfy.oq372', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:25', '2025-09-11 20:46:25');
INSERT INTO `raffle_activity_account_month` VALUES (189, 'ggqlnf_fo574', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:26', '2025-09-11 20:46:26');
INSERT INTO `raffle_activity_account_month` VALUES (190, 'i1clo517', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:27', '2025-09-11 20:46:27');
INSERT INTO `raffle_activity_account_month` VALUES (191, 'lm1sds_gii61', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:28', '2025-09-11 20:46:28');
INSERT INTO `raffle_activity_account_month` VALUES (192, 'pu9ggq_s3t71', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:29', '2025-09-11 20:46:29');
INSERT INTO `raffle_activity_account_month` VALUES (193, 'tbfv9r91', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:29', '2025-09-11 20:46:29');
INSERT INTO `raffle_activity_account_month` VALUES (194, 'gncsbt25', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:30', '2025-09-11 20:46:30');
INSERT INTO `raffle_activity_account_month` VALUES (195, 'gncsbt51', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:31', '2025-09-11 20:46:31');
INSERT INTO `raffle_activity_account_month` VALUES (196, 'hm1p8q.mtn', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:32', '2025-09-11 20:46:32');
INSERT INTO `raffle_activity_account_month` VALUES (197, 'tgmo3v.ptd0', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:33', '2025-09-11 20:46:33');
INSERT INTO `raffle_activity_account_month` VALUES (198, 'tbfnuy.fof31', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:33', '2025-09-11 20:46:33');
INSERT INTO `raffle_activity_account_month` VALUES (199, 'ob5j6i.vcr10', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:34', '2025-09-11 20:46:34');
INSERT INTO `raffle_activity_account_month` VALUES (200, 'jlbi46.g30i1l', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:35', '2025-09-11 20:46:35');
INSERT INTO `raffle_activity_account_month` VALUES (201, 'gs2k34.fgw', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:36', '2025-09-11 20:46:36');
INSERT INTO `raffle_activity_account_month` VALUES (202, 'k1zu0c.fgs', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:37', '2025-09-11 20:46:37');
INSERT INTO `raffle_activity_account_month` VALUES (203, 'v9rlnf92', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:37', '2025-09-11 20:46:37');
INSERT INTO `raffle_activity_account_month` VALUES (204, 'mawgge_itu', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:38', '2025-09-11 20:46:38');
INSERT INTO `raffle_activity_account_month` VALUES (205, 'k5qgs270', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:39', '2025-09-11 20:46:39');
INSERT INTO `raffle_activity_account_month` VALUES (206, 'jlbffg15', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:40', '2025-09-11 20:46:40');
INSERT INTO `raffle_activity_account_month` VALUES (207, 'k4ti7b21', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:41', '2025-09-11 20:46:41');
INSERT INTO `raffle_activity_account_month` VALUES (208, 'uclgm3.nh4', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:42', '2025-09-11 20:46:42');
INSERT INTO `raffle_activity_account_month` VALUES (209, 'v9ru7274', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:42', '2025-09-11 20:46:42');
INSERT INTO `raffle_activity_account_month` VALUES (210, 'uclgm3_hs4', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:43', '2025-09-11 20:46:43');
INSERT INTO `raffle_activity_account_month` VALUES (211, 'v9rmdm.ui4', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:44', '2025-09-11 20:46:44');
INSERT INTO `raffle_activity_account_month` VALUES (212, 'rydkjv_kjv91', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:45', '2025-09-11 20:46:45');
INSERT INTO `raffle_activity_account_month` VALUES (213, 'k7znlr38', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:46', '2025-09-11 20:46:46');
INSERT INTO `raffle_activity_account_month` VALUES (214, 'p1pscp_fo50', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:47', '2025-09-11 20:46:47');
INSERT INTO `raffle_activity_account_month` VALUES (215, 'irescp.lel27', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:47', '2025-09-11 20:46:47');
INSERT INTO `raffle_activity_account_month` VALUES (216, 'ts5tbf43', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:48', '2025-09-11 20:46:48');
INSERT INTO `raffle_activity_account_month` VALUES (217, 'my250', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:49', '2025-09-11 20:46:49');
INSERT INTO `raffle_activity_account_month` VALUES (218, 'freui8.kgq', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:50', '2025-09-11 20:46:50');
INSERT INTO `raffle_activity_account_month` VALUES (219, 'lm1_sny', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:51', '2025-09-11 20:46:51');
INSERT INTO `raffle_activity_account_month` VALUES (220, 'jlbffg.nky48', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:51', '2025-09-11 20:46:51');
INSERT INTO `raffle_activity_account_month` VALUES (221, 'mawfmn_tm050', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:52', '2025-09-11 20:46:52');
INSERT INTO `raffle_activity_account_month` VALUES (222, 'fmnrmi35', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:53', '2025-09-11 20:46:53');
INSERT INTO `raffle_activity_account_month` VALUES (223, 'k7znlr_go8', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:54', '2025-09-11 20:46:54');
INSERT INTO `raffle_activity_account_month` VALUES (224, 'k8qlqi.lld83', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:55', '2025-09-11 20:46:55');
INSERT INTO `raffle_activity_account_month` VALUES (225, 'k5qgs271', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:56', '2025-09-11 20:46:56');
INSERT INTO `raffle_activity_account_month` VALUES (226, 'k5mmd2.fg017', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:57', '2025-09-11 20:46:57');
INSERT INTO `raffle_activity_account_month` VALUES (227, 'mg0iuc26', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:58', '2025-09-11 20:46:58');
INSERT INTO `raffle_activity_account_month` VALUES (228, 'k8qkfk82', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:59', '2025-09-11 20:46:59');
INSERT INTO `raffle_activity_account_month` VALUES (229, 'l7nrqi26', 100301, '2025-09', 1000, 999, '2025-09-11 20:46:59', '2025-09-11 20:46:59');
INSERT INTO `raffle_activity_account_month` VALUES (230, 'ttbtw9_k6731', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:00', '2025-09-11 20:47:00');
INSERT INTO `raffle_activity_account_month` VALUES (231, 'irescp_gl4o6y', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:01', '2025-09-11 20:47:01');
INSERT INTO `raffle_activity_account_month` VALUES (232, 'ob5j6i_nevn5742', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:02', '2025-09-11 20:47:02');
INSERT INTO `raffle_activity_account_month` VALUES (233, 'fmni4885', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:03', '2025-09-11 20:47:03');
INSERT INTO `raffle_activity_account_month` VALUES (234, 'ppm_g3a46', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:03', '2025-09-11 20:47:03');
INSERT INTO `raffle_activity_account_month` VALUES (235, 'fevfoz96', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:04', '2025-09-11 20:47:04');
INSERT INTO `raffle_activity_account_month` VALUES (236, 'tw6my2.roi', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:05', '2025-09-11 20:47:05');
INSERT INTO `raffle_activity_account_month` VALUES (237, 'v9rmdm_fevfrz12', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:06', '2025-09-11 20:47:06');
INSERT INTO `raffle_activity_account_month` VALUES (238, 'k7znlr48', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:07', '2025-09-11 20:47:07');
INSERT INTO `raffle_activity_account_month` VALUES (239, 'ggqryd.twz81', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:07', '2025-09-11 20:47:07');
INSERT INTO `raffle_activity_account_month` VALUES (240, 'n0emzg_tog', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:08', '2025-09-11 20:47:08');
INSERT INTO `raffle_activity_account_month` VALUES (241, 'o97rmi.gbf98', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:09', '2025-09-11 20:47:09');
INSERT INTO `raffle_activity_account_month` VALUES (242, 'pd6fvp_gky51', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:10', '2025-09-11 20:47:10');
INSERT INTO `raffle_activity_account_month` VALUES (243, 'p72k3d_qjm', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:11', '2025-09-11 20:47:11');
INSERT INTO `raffle_activity_account_month` VALUES (244, 'me4hce.sld', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:11', '2025-09-11 20:47:11');
INSERT INTO `raffle_activity_account_month` VALUES (245, 'fmnrmi_vcr', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:12', '2025-09-11 20:47:12');
INSERT INTO `raffle_activity_account_month` VALUES (246, 'tt6kfy_nyg', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:13', '2025-09-11 20:47:13');
INSERT INTO `raffle_activity_account_month` VALUES (247, 'fozmy2_o9x53', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:14', '2025-09-11 20:47:14');
INSERT INTO `raffle_activity_account_month` VALUES (248, 'pd6fvp_shb', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:15', '2025-09-11 20:47:15');
INSERT INTO `raffle_activity_account_month` VALUES (249, 'gs2k345', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:15', '2025-09-11 20:47:15');
INSERT INTO `raffle_activity_account_month` VALUES (250, 'izhlq416', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:16', '2025-09-11 20:47:16');
INSERT INTO `raffle_activity_account_month` VALUES (251, 'fmnlj154', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:17', '2025-09-11 20:47:17');
INSERT INTO `raffle_activity_account_month` VALUES (252, 'tbfv9r.ul4', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:18', '2025-09-11 20:47:18');
INSERT INTO `raffle_activity_account_month` VALUES (253, 'fextfv63', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:19', '2025-09-11 20:47:19');
INSERT INTO `raffle_activity_account_month` VALUES (254, 'lm182', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:19', '2025-09-11 20:47:19');
INSERT INTO `raffle_activity_account_month` VALUES (255, 'jlbffg92', 100301, '2025-09', 1000, 999, '2025-09-11 20:47:20', '2025-09-11 20:47:20');

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抽奖活动单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of raffle_activity_order_000
-- ----------------------------
INSERT INTO `raffle_activity_order_000` VALUES (1, 'xiaofuge1', 9011, 100301, '测试活动', 100006, '831917125310', '2024-05-03 08:01:44', 10, 10, 10, NULL, 'completed', 'xiaofuge1_sku_20240503', '2024-05-03 16:01:44', '2024-05-03 16:01:44');
INSERT INTO `raffle_activity_order_000` VALUES (2, 'user001', 9011, 100301, '测试活动', 100006, '923549663927', '2024-05-24 14:30:55', 100, 100, 100, NULL, 'completed', 'user001_sku_20240524', '2024-05-24 22:30:54', '2024-05-24 22:30:54');

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
) ENGINE = InnoDB AUTO_INCREMENT = 262 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抽奖活动单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of raffle_activity_order_001
-- ----------------------------
INSERT INTO `raffle_activity_order_001` VALUES (3, 'xiaofuge', 9011, 100301, '测试活动', 100006, '383240888158', '2024-03-23 04:38:23', 1, 1, 1, NULL, 'completed', '700091009111', '2024-03-23 12:38:23', '2024-03-23 12:38:23');
INSERT INTO `raffle_activity_order_001` VALUES (4, 'user002', 9011, 100301, '测试活动', 100006, '165083654323', '2024-05-24 14:30:55', 100, 100, 100, NULL, 'completed', 'user002_sku_20240524', '2024-05-24 22:30:54', '2024-05-24 22:30:54');
INSERT INTO `raffle_activity_order_001` VALUES (261, 'user002', 9011, 100301, '测试活动', 100006, '762873325216', '2024-05-27 14:42:18', 100, 100, 100, NULL, 'completed', 'user002_sku_20240527', '2024-05-27 22:42:17', '2024-05-27 22:42:17');

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抽奖活动单' ROW_FORMAT = Dynamic;

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
INSERT INTO `raffle_activity_order_003` VALUES (2, 'xiaofuge2', 9011, 100301, '测试活动', 100006, '942458887115', '2024-05-04 05:07:53', 10, 10, 10, NULL, 'completed', 'xiaofuge2_sku_20240504', '2024-05-04 13:07:53', '2024-05-04 13:07:53');

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
) ENGINE = InnoDB AUTO_INCREMENT = 253 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务表，发送MQ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of task
-- ----------------------------
INSERT INTO `task` VALUES (1, 'fgpsbt_go848', 'send_award', '70589861744', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"563332000698\",\"userId\":\"fgpsbt_go848\"},\"id\":\"70589861744\",\"timestamp\":\"2025-09-11 20:43:55.674\"}', 'completed', '2025-09-11 20:43:55', '2025-09-11 20:43:55');
INSERT INTO `task` VALUES (2, 'mg0iuc15', 'send_award', '59559572397', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"751045921382\",\"userId\":\"mg0iuc15\"},\"id\":\"59559572397\",\"timestamp\":\"2025-09-11 20:43:56.506\"}', 'completed', '2025-09-11 20:43:56', '2025-09-11 20:43:56');
INSERT INTO `task` VALUES (3, 'k1zggq91', 'send_award', '73943685394', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"903249703262\",\"userId\":\"k1zggq91\"},\"id\":\"73943685394\",\"timestamp\":\"2025-09-11 20:43:57.373\"}', 'completed', '2025-09-11 20:43:57', '2025-09-11 20:43:57');
INSERT INTO `task` VALUES (4, 'ppm52', 'send_award', '95914959581', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"089122622827\",\"userId\":\"ppm52\"},\"id\":\"95914959581\",\"timestamp\":\"2025-09-11 20:43:58.13\"}', 'completed', '2025-09-11 20:43:58', '2025-09-11 20:43:58');
INSERT INTO `task` VALUES (5, 'm8eiuk.ssq48', 'send_award', '75074262256', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"076052722368\",\"userId\":\"m8eiuk.ssq48\"},\"id\":\"75074262256\",\"timestamp\":\"2025-09-11 20:43:58.919\"}', 'completed', '2025-09-11 20:43:59', '2025-09-11 20:43:59');
INSERT INTO `task` VALUES (6, 'pu9ggq31', 'send_award', '22151440078', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"473878697896\",\"userId\":\"pu9ggq31\"},\"id\":\"22151440078\",\"timestamp\":\"2025-09-11 20:43:59.734\"}', 'completed', '2025-09-11 20:43:59', '2025-09-11 20:43:59');
INSERT INTO `task` VALUES (7, 'izhlq4_gii', 'send_award', '11374650095', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"937148513755\",\"userId\":\"izhlq4_gii\"},\"id\":\"11374650095\",\"timestamp\":\"2025-09-11 20:44:00.549\"}', 'completed', '2025-09-11 20:44:00', '2025-09-11 20:44:00');
INSERT INTO `task` VALUES (8, 'jzzgm357', 'send_award', '50279790181', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"285290209479\",\"userId\":\"jzzgm357\"},\"id\":\"50279790181\",\"timestamp\":\"2025-09-11 20:44:01.32\"}', 'completed', '2025-09-11 20:44:01', '2025-09-11 20:44:01');
INSERT INTO `task` VALUES (9, 'v9blnf.uc9', 'send_award', '97194544269', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"101549619845\",\"userId\":\"v9blnf.uc9\"},\"id\":\"97194544269\",\"timestamp\":\"2025-09-11 20:44:02.159\"}', 'completed', '2025-09-11 20:44:02', '2025-09-11 20:44:02');
INSERT INTO `task` VALUES (10, 'kgs62', 'send_award', '82824860418', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"833715285056\",\"userId\":\"kgs62\"},\"id\":\"82824860418\",\"timestamp\":\"2025-09-11 20:44:02.978\"}', 'completed', '2025-09-11 20:44:03', '2025-09-11 20:44:03');
INSERT INTO `task` VALUES (11, 'o97fmn_q0b37', 'send_award', '86068262654', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"714351845530\",\"userId\":\"o97fmn_q0b37\"},\"id\":\"86068262654\",\"timestamp\":\"2025-09-11 20:44:03.835\"}', 'completed', '2025-09-11 20:44:03', '2025-09-11 20:44:04');
INSERT INTO `task` VALUES (12, 'gztmft40', 'send_award', '13528992180', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"196080466656\",\"userId\":\"gztmft40\"},\"id\":\"13528992180\",\"timestamp\":\"2025-09-11 20:44:04.634\"}', 'completed', '2025-09-11 20:44:04', '2025-09-11 20:44:04');
INSERT INTO `task` VALUES (13, 'izhlq4.rxn', 'send_award', '68109671110', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"392717571649\",\"userId\":\"izhlq4.rxn\"},\"id\":\"68109671110\",\"timestamp\":\"2025-09-11 20:44:05.472\"}', 'completed', '2025-09-11 20:44:05', '2025-09-11 20:44:05');
INSERT INTO `task` VALUES (14, 'tbfnuy.vgw', 'send_award', '00182048172', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"842809108006\",\"userId\":\"tbfnuy.vgw\"},\"id\":\"00182048172\",\"timestamp\":\"2025-09-11 20:44:06.241\"}', 'completed', '2025-09-11 20:44:06', '2025-09-11 20:44:06');
INSERT INTO `task` VALUES (15, 'o97kgs32', 'send_award', '62166252959', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"712724533387\",\"userId\":\"o97kgs32\"},\"id\":\"62166252959\",\"timestamp\":\"2025-09-11 20:44:07.093\"}', 'completed', '2025-09-11 20:44:07', '2025-09-11 20:44:07');
INSERT INTO `task` VALUES (16, 'ggqlnf_tca', 'send_award', '56371728296', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"363170555865\",\"userId\":\"ggqlnf_tca\"},\"id\":\"56371728296\",\"timestamp\":\"2025-09-11 20:44:07.908\"}', 'completed', '2025-09-11 20:44:08', '2025-09-11 20:44:08');
INSERT INTO `task` VALUES (17, 'jlbffg.tu527', 'send_award', '73591304791', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"591801031170\",\"userId\":\"jlbffg.tu527\"},\"id\":\"73591304791\",\"timestamp\":\"2025-09-11 20:44:08.685\"}', 'completed', '2025-09-11 20:44:08', '2025-09-11 20:44:08');
INSERT INTO `task` VALUES (18, 'izhlu897', 'send_award', '74028559221', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"643627510170\",\"userId\":\"izhlu897\"},\"id\":\"74028559221\",\"timestamp\":\"2025-09-11 20:44:09.531\"}', 'completed', '2025-09-11 20:44:09', '2025-09-11 20:44:09');
INSERT INTO `task` VALUES (19, 'i2xmy2.r8j', 'send_award', '80747019672', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"361751462619\",\"userId\":\"i2xmy2.r8j\"},\"id\":\"80747019672\",\"timestamp\":\"2025-09-11 20:44:10.647\"}', 'completed', '2025-09-11 20:44:10', '2025-09-11 20:44:10');
INSERT INTO `task` VALUES (20, 'ggqk1z.ijv31', 'send_award', '79727287230', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"244288706341\",\"userId\":\"ggqk1z.ijv31\"},\"id\":\"79727287230\",\"timestamp\":\"2025-09-11 20:44:11.451\"}', 'completed', '2025-09-11 20:44:11', '2025-09-11 20:44:11');
INSERT INTO `task` VALUES (21, 'freg6n24', 'send_award', '37701827846', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"599728785608\",\"userId\":\"freg6n24\"},\"id\":\"37701827846\",\"timestamp\":\"2025-09-11 20:44:12.315\"}', 'completed', '2025-09-11 20:44:12', '2025-09-11 20:44:12');
INSERT INTO `task` VALUES (22, 'k8qi488', 'send_award', '93398596214', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"989817973678\",\"userId\":\"k8qi488\"},\"id\":\"93398596214\",\"timestamp\":\"2025-09-11 20:44:13.18\"}', 'completed', '2025-09-11 20:44:13', '2025-09-11 20:44:13');
INSERT INTO `task` VALUES (23, 'ryiiuk88', 'send_award', '60203944260', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"673171703022\",\"userId\":\"ryiiuk88\"},\"id\":\"60203944260\",\"timestamp\":\"2025-09-11 20:44:14\"}', 'completed', '2025-09-11 20:44:14', '2025-09-11 20:44:14');
INSERT INTO `task` VALUES (24, 'lm1sbt_seo', 'send_award', '55834970539', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"060715855252\",\"userId\":\"lm1sbt_seo\"},\"id\":\"55834970539\",\"timestamp\":\"2025-09-11 20:44:14.823\"}', 'completed', '2025-09-11 20:44:14', '2025-09-11 20:44:15');
INSERT INTO `task` VALUES (25, 'me4nzu4', 'send_award', '53856661891', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"168450943720\",\"userId\":\"me4nzu4\"},\"id\":\"53856661891\",\"timestamp\":\"2025-09-11 20:44:15.626\"}', 'completed', '2025-09-11 20:44:15', '2025-09-11 20:44:15');
INSERT INTO `task` VALUES (26, 'k8qscp8', 'send_award', '26693242097', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"353860910031\",\"userId\":\"k8qscp8\"},\"id\":\"26693242097\",\"timestamp\":\"2025-09-11 20:44:16.542\"}', 'completed', '2025-09-11 20:44:16', '2025-09-11 20:44:16');
INSERT INTO `task` VALUES (27, 'o97sbt73', 'send_award', '52097287964', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"961670415428\",\"userId\":\"o97sbt73\"},\"id\":\"52097287964\",\"timestamp\":\"2025-09-11 20:44:17.398\"}', 'completed', '2025-09-11 20:44:17', '2025-09-11 20:44:17');
INSERT INTO `task` VALUES (28, 'izh38', 'send_award', '56879463704', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"130835750784\",\"userId\":\"izh38\"},\"id\":\"56879463704\",\"timestamp\":\"2025-09-11 20:44:18.502\"}', 'completed', '2025-09-11 20:44:18', '2025-09-11 20:44:18');
INSERT INTO `task` VALUES (29, 'lm1sds.fevfrz', 'send_award', '55503676794', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"807204192857\",\"userId\":\"lm1sds.fevfrz\"},\"id\":\"55503676794\",\"timestamp\":\"2025-09-11 20:44:19.28\"}', 'completed', '2025-09-11 20:44:19', '2025-09-11 20:44:19');
INSERT INTO `task` VALUES (30, 'lmfmhh36', 'send_award', '07211782733', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"275814493901\",\"userId\":\"lmfmhh36\"},\"id\":\"07211782733\",\"timestamp\":\"2025-09-11 20:44:20.095\"}', 'completed', '2025-09-11 20:44:20', '2025-09-11 20:44:20');
INSERT INTO `task` VALUES (31, 'm97k5q34', 'send_award', '81561026267', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"532317029989\",\"userId\":\"m97k5q34\"},\"id\":\"81561026267\",\"timestamp\":\"2025-09-11 20:44:20.925\"}', 'completed', '2025-09-11 20:44:21', '2025-09-11 20:44:21');
INSERT INTO `task` VALUES (32, 'fvpki783', 'send_award', '68199475371', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"226303920407\",\"userId\":\"fvpki783\"},\"id\":\"68199475371\",\"timestamp\":\"2025-09-11 20:44:22.428\"}', 'completed', '2025-09-11 20:44:22', '2025-09-11 20:44:22');
INSERT INTO `task` VALUES (33, 'feog2w_pet4', 'send_award', '76339744827', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"049233571966\",\"userId\":\"feog2w_pet4\"},\"id\":\"76339744827\",\"timestamp\":\"2025-09-11 20:44:23.192\"}', 'completed', '2025-09-11 20:44:23', '2025-09-11 20:44:23');
INSERT INTO `task` VALUES (34, 'pa8kfk.gk458', 'send_award', '78939952114', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"032030798498\",\"userId\":\"pa8kfk.gk458\"},\"id\":\"78939952114\",\"timestamp\":\"2025-09-11 20:44:24.003\"}', 'completed', '2025-09-11 20:44:24', '2025-09-11 20:44:24');
INSERT INTO `task` VALUES (35, 'tbfnuy_gcf', 'send_award', '42771999199', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"763544290330\",\"userId\":\"tbfnuy_gcf\"},\"id\":\"42771999199\",\"timestamp\":\"2025-09-11 20:44:24.84\"}', 'completed', '2025-09-11 20:44:24', '2025-09-11 20:44:24');
INSERT INTO `task` VALUES (36, 'k1zggq8', 'send_award', '68493811354', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"139380037709\",\"userId\":\"k1zggq8\"},\"id\":\"68493811354\",\"timestamp\":\"2025-09-11 20:44:25.637\"}', 'completed', '2025-09-11 20:44:25', '2025-09-11 20:44:25');
INSERT INTO `task` VALUES (37, 'tbfnuy.o8678', 'send_award', '79928882984', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"468320298081\",\"userId\":\"tbfnuy.o8678\"},\"id\":\"79928882984\",\"timestamp\":\"2025-09-11 20:44:26.404\"}', 'completed', '2025-09-11 20:44:26', '2025-09-11 20:44:26');
INSERT INTO `task` VALUES (38, 'i1cl5v_oa71', 'send_award', '81135969456', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"440944749054\",\"userId\":\"i1cl5v_oa71\"},\"id\":\"81135969456\",\"timestamp\":\"2025-09-11 20:44:27.2\"}', 'completed', '2025-09-11 20:44:27', '2025-09-11 20:44:27');
INSERT INTO `task` VALUES (39, 'k18iui33', 'send_award', '22006786364', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"247491259114\",\"userId\":\"k18iui33\"},\"id\":\"22006786364\",\"timestamp\":\"2025-09-11 20:44:27.993\"}', 'completed', '2025-09-11 20:44:28', '2025-09-11 20:44:28');
INSERT INTO `task` VALUES (40, 'freg6n.pu571', 'send_award', '90539802268', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"054695370315\",\"userId\":\"freg6n.pu571\"},\"id\":\"90539802268\",\"timestamp\":\"2025-09-11 20:44:28.793\"}', 'completed', '2025-09-11 20:44:28', '2025-09-11 20:44:28');
INSERT INTO `task` VALUES (41, 'ucop8d_skb26', 'send_award', '97136421050', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"847493615368\",\"userId\":\"ucop8d_skb26\"},\"id\":\"97136421050\",\"timestamp\":\"2025-09-11 20:44:29.526\"}', 'completed', '2025-09-11 20:44:29', '2025-09-11 20:44:29');
INSERT INTO `task` VALUES (42, 'ts5hvb.oti', 'send_award', '92034903360', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"858516683395\",\"userId\":\"ts5hvb.oti\"},\"id\":\"92034903360\",\"timestamp\":\"2025-09-11 20:44:30.428\"}', 'completed', '2025-09-11 20:44:30', '2025-09-11 20:44:30');
INSERT INTO `task` VALUES (43, 'k88to3_fj9', 'send_award', '05264998640', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"275822482545\",\"userId\":\"k88to3_fj9\"},\"id\":\"05264998640\",\"timestamp\":\"2025-09-11 20:44:31.237\"}', 'completed', '2025-09-11 20:44:31', '2025-09-11 20:44:31');
INSERT INTO `task` VALUES (44, 'jzzgm3_kls', 'send_award', '04680391503', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"601722234977\",\"userId\":\"jzzgm3_kls\"},\"id\":\"04680391503\",\"timestamp\":\"2025-09-11 20:44:32.03\"}', 'completed', '2025-09-11 20:44:32', '2025-09-11 20:44:32');
INSERT INTO `task` VALUES (45, 'ui8_noz35', 'send_award', '53602927361', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"160126513297\",\"userId\":\"ui8_noz35\"},\"id\":\"53602927361\",\"timestamp\":\"2025-09-11 20:44:32.873\"}', 'completed', '2025-09-11 20:44:32', '2025-09-11 20:44:33');
INSERT INTO `task` VALUES (46, 'hm1p8q25', 'send_award', '88249646073', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"695015150623\",\"userId\":\"hm1p8q25\"},\"id\":\"88249646073\",\"timestamp\":\"2025-09-11 20:44:33.707\"}', 'completed', '2025-09-11 20:44:33', '2025-09-11 20:44:33');
INSERT INTO `task` VALUES (47, 'izhsf0.nky82', 'send_award', '26766684108', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"760840137427\",\"userId\":\"izhsf0.nky82\"},\"id\":\"26766684108\",\"timestamp\":\"2025-09-11 20:44:34.531\"}', 'completed', '2025-09-11 20:44:34', '2025-09-11 20:44:34');
INSERT INTO `task` VALUES (48, 'v9ru72.px5', 'send_award', '95418116824', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"212127622355\",\"userId\":\"v9ru72.px5\"},\"id\":\"95418116824\",\"timestamp\":\"2025-09-11 20:44:35.35\"}', 'completed', '2025-09-11 20:44:35', '2025-09-11 20:44:35');
INSERT INTO `task` VALUES (49, 'lmfmhh_k28', 'send_award', '92995750942', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"526427902784\",\"userId\":\"lmfmhh_k28\"},\"id\":\"92995750942\",\"timestamp\":\"2025-09-11 20:44:36.092\"}', 'completed', '2025-09-11 20:44:36', '2025-09-11 20:44:36');
INSERT INTO `task` VALUES (50, 'ts5hvb63', 'send_award', '16595150904', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"611277238666\",\"userId\":\"ts5hvb63\"},\"id\":\"16595150904\",\"timestamp\":\"2025-09-11 20:44:36.884\"}', 'completed', '2025-09-11 20:44:36', '2025-09-11 20:44:37');
INSERT INTO `task` VALUES (51, 'vacsbt_fse16', 'send_award', '12725845213', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"009927625959\",\"userId\":\"vacsbt_fse16\"},\"id\":\"12725845213\",\"timestamp\":\"2025-09-11 20:44:37.678\"}', 'completed', '2025-09-11 20:44:37', '2025-09-11 20:44:37');
INSERT INTO `task` VALUES (52, 'jwui2v_g1y56', 'send_award', '81082561515', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"586799642017\",\"userId\":\"jwui2v_g1y56\"},\"id\":\"81082561515\",\"timestamp\":\"2025-09-11 20:44:38.465\"}', 'completed', '2025-09-11 20:44:38', '2025-09-11 20:44:38');
INSERT INTO `task` VALUES (53, 'n0emzg_q0b61', 'send_award', '77324101968', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"014082357956\",\"userId\":\"n0emzg_q0b61\"},\"id\":\"77324101968\",\"timestamp\":\"2025-09-11 20:44:39.296\"}', 'completed', '2025-09-11 20:44:39', '2025-09-11 20:44:39');
INSERT INTO `task` VALUES (54, 'ixjit6_tom', 'send_award', '03957359889', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"838603739738\",\"userId\":\"ixjit6_tom\"},\"id\":\"03957359889\",\"timestamp\":\"2025-09-11 20:44:40.047\"}', 'completed', '2025-09-11 20:44:40', '2025-09-11 20:44:40');
INSERT INTO `task` VALUES (55, 'fevfoz_pca37', 'send_award', '32173010767', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"494353821056\",\"userId\":\"fevfoz_pca37\"},\"id\":\"32173010767\",\"timestamp\":\"2025-09-11 20:44:40.881\"}', 'completed', '2025-09-11 20:44:40', '2025-09-11 20:44:41');
INSERT INTO `task` VALUES (56, 'lh7k1z_go8', 'send_award', '11180543668', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"952614189678\",\"userId\":\"lh7k1z_go8\"},\"id\":\"11180543668\",\"timestamp\":\"2025-09-11 20:44:41.748\"}', 'completed', '2025-09-11 20:44:41', '2025-09-11 20:44:41');
INSERT INTO `task` VALUES (57, 'mawtue.tfa', 'send_award', '29590989356', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"761928899734\",\"userId\":\"mawtue.tfa\"},\"id\":\"29590989356\",\"timestamp\":\"2025-09-11 20:44:42.593\"}', 'completed', '2025-09-11 20:44:42', '2025-09-11 20:44:42');
INSERT INTO `task` VALUES (58, 'k8qi48_lid81', 'send_award', '48372899608', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"936919063532\",\"userId\":\"k8qi48_lid81\"},\"id\":\"48372899608\",\"timestamp\":\"2025-09-11 20:44:43.429\"}', 'completed', '2025-09-11 20:44:43', '2025-09-11 20:44:43');
INSERT INTO `task` VALUES (59, 'mawgge40', 'send_award', '71329070552', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"234983687736\",\"userId\":\"mawgge40\"},\"id\":\"71329070552\",\"timestamp\":\"2025-09-11 20:44:44.353\"}', 'completed', '2025-09-11 20:44:44', '2025-09-11 20:44:44');
INSERT INTO `task` VALUES (60, 'n0emzg_kfz25', 'send_award', '96665055656', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"002259037063\",\"userId\":\"n0emzg_kfz25\"},\"id\":\"96665055656\",\"timestamp\":\"2025-09-11 20:44:45.116\"}', 'completed', '2025-09-11 20:44:45', '2025-09-11 20:44:45');
INSERT INTO `task` VALUES (61, 'ob5j6i_g7u34', 'send_award', '58628118970', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"013367367828\",\"userId\":\"ob5j6i_g7u34\"},\"id\":\"58628118970\",\"timestamp\":\"2025-09-11 20:44:45.948\"}', 'completed', '2025-09-11 20:44:46', '2025-09-11 20:44:46');
INSERT INTO `task` VALUES (62, 'fmni483', 'send_award', '42640771197', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"361952775166\",\"userId\":\"fmni483\"},\"id\":\"42640771197\",\"timestamp\":\"2025-09-11 20:44:46.899\"}', 'completed', '2025-09-11 20:44:46', '2025-09-11 20:44:47');
INSERT INTO `task` VALUES (63, 'v9bmdm_u0617', 'send_award', '75330305326', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"877777181543\",\"userId\":\"v9bmdm_u0617\"},\"id\":\"75330305326\",\"timestamp\":\"2025-09-11 20:44:47.715\"}', 'completed', '2025-09-11 20:44:47', '2025-09-11 20:44:47');
INSERT INTO `task` VALUES (64, 'gncsbt31', 'send_award', '62809711823', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"004062557558\",\"userId\":\"gncsbt31\"},\"id\":\"62809711823\",\"timestamp\":\"2025-09-11 20:44:48.547\"}', 'completed', '2025-09-11 20:44:48', '2025-09-11 20:44:48');
INSERT INTO `task` VALUES (65, 'lh7lnf81', 'send_award', '49919865511', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"047229855366\",\"userId\":\"lh7lnf81\"},\"id\":\"49919865511\",\"timestamp\":\"2025-09-11 20:44:49.345\"}', 'completed', '2025-09-11 20:44:49', '2025-09-11 20:44:49');
INSERT INTO `task` VALUES (66, 'ixjit6.ldu80', 'send_award', '69557484289', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"278057521875\",\"userId\":\"ixjit6.ldu80\"},\"id\":\"69557484289\",\"timestamp\":\"2025-09-11 20:44:50.149\"}', 'completed', '2025-09-11 20:44:50', '2025-09-11 20:44:50');
INSERT INTO `task` VALUES (67, 'k5qscp66', 'send_award', '30495477286', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"780419895861\",\"userId\":\"k5qscp66\"},\"id\":\"30495477286\",\"timestamp\":\"2025-09-11 20:44:50.929\"}', 'completed', '2025-09-11 20:44:51', '2025-09-11 20:44:51');
INSERT INTO `task` VALUES (68, 'lm1sbt53', 'send_award', '53969901504', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"264201005141\",\"userId\":\"lm1sbt53\"},\"id\":\"53969901504\",\"timestamp\":\"2025-09-11 20:44:51.76\"}', 'completed', '2025-09-11 20:44:51', '2025-09-11 20:44:51');
INSERT INTO `task` VALUES (69, 'i1clo591', 'send_award', '50436836633', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"036385657654\",\"userId\":\"i1clo591\"},\"id\":\"50436836633\",\"timestamp\":\"2025-09-11 20:44:52.533\"}', 'completed', '2025-09-11 20:44:52', '2025-09-11 20:44:52');
INSERT INTO `task` VALUES (70, 'ryilj176', 'send_award', '05989722764', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"880908108216\",\"userId\":\"ryilj176\"},\"id\":\"05989722764\",\"timestamp\":\"2025-09-11 20:44:53.318\"}', 'completed', '2025-09-11 20:44:53', '2025-09-11 20:44:53');
INSERT INTO `task` VALUES (71, 'i1cvdk30', 'send_award', '74817201510', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"556689350107\",\"userId\":\"i1cvdk30\"},\"id\":\"74817201510\",\"timestamp\":\"2025-09-11 20:44:54.143\"}', 'completed', '2025-09-11 20:44:54', '2025-09-11 20:44:54');
INSERT INTO `task` VALUES (72, 'irescp_lef33', 'send_award', '50308425885', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"090265060035\",\"userId\":\"irescp_lef33\"},\"id\":\"50308425885\",\"timestamp\":\"2025-09-11 20:44:54.948\"}', 'completed', '2025-09-11 20:44:55', '2025-09-11 20:44:55');
INSERT INTO `task` VALUES (73, 'i1csbt_n5f', 'send_award', '44820751710', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"608949369459\",\"userId\":\"i1csbt_n5f\"},\"id\":\"44820751710\",\"timestamp\":\"2025-09-11 20:44:55.745\"}', 'completed', '2025-09-11 20:44:55', '2025-09-11 20:44:55');
INSERT INTO `task` VALUES (74, 'ggqryd.rwe1', 'send_award', '40813690895', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"080027856118\",\"userId\":\"ggqryd.rwe1\"},\"id\":\"40813690895\",\"timestamp\":\"2025-09-11 20:44:56.505\"}', 'completed', '2025-09-11 20:44:56', '2025-09-11 20:44:56');
INSERT INTO `task` VALUES (75, 'lh7lnf87', 'send_award', '19306121669', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"074459947055\",\"userId\":\"lh7lnf87\"},\"id\":\"19306121669\",\"timestamp\":\"2025-09-11 20:44:57.281\"}', 'completed', '2025-09-11 20:44:57', '2025-09-11 20:44:57');
INSERT INTO `task` VALUES (76, 'oq3kgn_i6780', 'send_award', '34554478746', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"491662474781\",\"userId\":\"oq3kgn_i6780\"},\"id\":\"34554478746\",\"timestamp\":\"2025-09-11 20:44:58.076\"}', 'completed', '2025-09-11 20:44:58', '2025-09-11 20:44:58');
INSERT INTO `task` VALUES (77, 'o97scp.gp8', 'send_award', '04972561966', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"224063784769\",\"userId\":\"o97scp.gp8\"},\"id\":\"04972561966\",\"timestamp\":\"2025-09-11 20:44:58.835\"}', 'completed', '2025-09-11 20:44:58', '2025-09-11 20:44:58');
INSERT INTO `task` VALUES (78, 'i1cuji.kdw21', 'send_award', '73545562599', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"131812513921\",\"userId\":\"i1cuji.kdw21\"},\"id\":\"73545562599\",\"timestamp\":\"2025-09-11 20:44:59.602\"}', 'completed', '2025-09-11 20:44:59', '2025-09-11 20:44:59');
INSERT INTO `task` VALUES (79, 'freui869', 'send_award', '29500251772', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"579729086379\",\"userId\":\"freui869\"},\"id\":\"29500251772\",\"timestamp\":\"2025-09-11 20:45:00.411\"}', 'completed', '2025-09-11 20:45:00', '2025-09-11 20:45:00');
INSERT INTO `task` VALUES (80, 'kgs.i60', 'send_award', '36141786468', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"896999071131\",\"userId\":\"kgs.i60\"},\"id\":\"36141786468\",\"timestamp\":\"2025-09-11 20:45:01.174\"}', 'completed', '2025-09-11 20:45:01', '2025-09-11 20:45:01');
INSERT INTO `task` VALUES (81, 'k7znlr46', 'send_award', '95402498799', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"539505157720\",\"userId\":\"k7znlr46\"},\"id\":\"95402498799\",\"timestamp\":\"2025-09-11 20:45:01.994\"}', 'completed', '2025-09-11 20:45:02', '2025-09-11 20:45:02');
INSERT INTO `task` VALUES (82, 'fextfv_fip68', 'send_award', '49000012545', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"714829104240\",\"userId\":\"fextfv_fip68\"},\"id\":\"49000012545\",\"timestamp\":\"2025-09-11 20:45:02.806\"}', 'completed', '2025-09-11 20:45:02', '2025-09-11 20:45:02');
INSERT INTO `task` VALUES (83, 'p1pvgw.fkp22', 'send_award', '87351861356', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"927384341281\",\"userId\":\"p1pvgw.fkp22\"},\"id\":\"87351861356\",\"timestamp\":\"2025-09-11 20:45:03.631\"}', 'completed', '2025-09-11 20:45:03', '2025-09-11 20:45:03');
INSERT INTO `task` VALUES (84, 'i1csbt70', 'send_award', '22493951101', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"561507325512\",\"userId\":\"i1csbt70\"},\"id\":\"22493951101\",\"timestamp\":\"2025-09-11 20:45:04.4\"}', 'completed', '2025-09-11 20:45:04', '2025-09-11 20:45:04');
INSERT INTO `task` VALUES (85, 'ryiiuk.ml733', 'send_award', '62992835315', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"720537053052\",\"userId\":\"ryiiuk.ml733\"},\"id\":\"62992835315\",\"timestamp\":\"2025-09-11 20:45:05.224\"}', 'completed', '2025-09-11 20:45:05', '2025-09-11 20:45:05');
INSERT INTO `task` VALUES (86, 'irescp.fo890', 'send_award', '99330052857', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"548845045615\",\"userId\":\"irescp.fo890\"},\"id\":\"99330052857\",\"timestamp\":\"2025-09-11 20:45:06.005\"}', 'completed', '2025-09-11 20:45:06', '2025-09-11 20:45:06');
INSERT INTO `task` VALUES (87, 'mzitue_jql', 'send_award', '40891979010', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"052226621174\",\"userId\":\"mzitue_jql\"},\"id\":\"40891979010\",\"timestamp\":\"2025-09-11 20:45:06.813\"}', 'completed', '2025-09-11 20:45:06', '2025-09-11 20:45:07');
INSERT INTO `task` VALUES (88, 'l7nrqi_hrg60', 'send_award', '07193723283', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"053397780277\",\"userId\":\"l7nrqi_hrg60\"},\"id\":\"07193723283\",\"timestamp\":\"2025-09-11 20:45:07.619\"}', 'completed', '2025-09-11 20:45:07', '2025-09-11 20:45:07');
INSERT INTO `task` VALUES (89, 'tgmo3v.lpi', 'send_award', '20724597044', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"015206330380\",\"userId\":\"tgmo3v.lpi\"},\"id\":\"20724597044\",\"timestamp\":\"2025-09-11 20:45:08.426\"}', 'completed', '2025-09-11 20:45:08', '2025-09-11 20:45:08');
INSERT INTO `task` VALUES (90, 'lh7lnf.klt77', 'send_award', '65788433394', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"059644126934\",\"userId\":\"lh7lnf.klt77\"},\"id\":\"65788433394\",\"timestamp\":\"2025-09-11 20:45:09.247\"}', 'completed', '2025-09-11 20:45:09', '2025-09-11 20:45:09');
INSERT INTO `task` VALUES (91, 'jwui2v_jjl', 'send_award', '30403370533', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"585060132129\",\"userId\":\"jwui2v_jjl\"},\"id\":\"30403370533\",\"timestamp\":\"2025-09-11 20:45:10.007\"}', 'completed', '2025-09-11 20:45:10', '2025-09-11 20:45:10');
INSERT INTO `task` VALUES (92, 'p72k3d96', 'send_award', '72488349416', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"062168416291\",\"userId\":\"p72k3d96\"},\"id\":\"72488349416\",\"timestamp\":\"2025-09-11 20:45:10.848\"}', 'completed', '2025-09-11 20:45:10', '2025-09-11 20:45:11');
INSERT INTO `task` VALUES (93, 'ts5tbf_rvh19', 'send_award', '40945784245', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"205160331391\",\"userId\":\"ts5tbf_rvh19\"},\"id\":\"40945784245\",\"timestamp\":\"2025-09-11 20:45:11.625\"}', 'completed', '2025-09-11 20:45:11', '2025-09-11 20:45:11');
INSERT INTO `task` VALUES (94, 'lm1i2v6', 'send_award', '80119292666', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"160548526180\",\"userId\":\"lm1i2v6\"},\"id\":\"80119292666\",\"timestamp\":\"2025-09-11 20:45:12.359\"}', 'completed', '2025-09-11 20:45:12', '2025-09-11 20:45:12');
INSERT INTO `task` VALUES (95, 'ryiiuk.mlb49', 'send_award', '64742624646', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"376403963358\",\"userId\":\"ryiiuk.mlb49\"},\"id\":\"64742624646\",\"timestamp\":\"2025-09-11 20:45:13.159\"}', 'completed', '2025-09-11 20:45:13', '2025-09-11 20:45:13');
INSERT INTO `task` VALUES (96, 'tgmo3v_jd4', 'send_award', '66844549664', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"374338139968\",\"userId\":\"tgmo3v_jd4\"},\"id\":\"66844549664\",\"timestamp\":\"2025-09-11 20:45:13.948\"}', 'completed', '2025-09-11 20:45:14', '2025-09-11 20:45:14');
INSERT INTO `task` VALUES (97, 'i1cvdk27', 'send_award', '90416638751', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"969079391077\",\"userId\":\"i1cvdk27\"},\"id\":\"90416638751\",\"timestamp\":\"2025-09-11 20:45:14.703\"}', 'completed', '2025-09-11 20:45:14', '2025-09-11 20:45:14');
INSERT INTO `task` VALUES (98, 'k7fv9r96', 'send_award', '66830418519', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"192480101955\",\"userId\":\"k7fv9r96\"},\"id\":\"66830418519\",\"timestamp\":\"2025-09-11 20:45:15.475\"}', 'completed', '2025-09-11 20:45:15', '2025-09-11 20:45:15');
INSERT INTO `task` VALUES (99, 'ob5j6i22', 'send_award', '18026693292', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"428235789937\",\"userId\":\"ob5j6i22\"},\"id\":\"18026693292\",\"timestamp\":\"2025-09-11 20:45:16.533\"}', 'completed', '2025-09-11 20:45:16', '2025-09-11 20:45:16');
INSERT INTO `task` VALUES (100, 'my2_q11', 'send_award', '15591386114', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"903330690750\",\"userId\":\"my2_q11\"},\"id\":\"15591386114\",\"timestamp\":\"2025-09-11 20:45:17.306\"}', 'completed', '2025-09-11 20:45:17', '2025-09-11 20:45:17');
INSERT INTO `task` VALUES (101, 'ktzmzi69', 'send_award', '14695461288', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"181300844986\",\"userId\":\"ktzmzi69\"},\"id\":\"14695461288\",\"timestamp\":\"2025-09-11 20:45:18.095\"}', 'completed', '2025-09-11 20:45:18', '2025-09-11 20:45:18');
INSERT INTO `task` VALUES (102, 'o97scp.gju20', 'send_award', '74855997049', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"401414121250\",\"userId\":\"o97scp.gju20\"},\"id\":\"74855997049\",\"timestamp\":\"2025-09-11 20:45:18.945\"}', 'completed', '2025-09-11 20:45:19', '2025-09-11 20:45:19');
INSERT INTO `task` VALUES (103, 'ggqlnf69', 'send_award', '55032953823', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"305883833827\",\"userId\":\"ggqlnf69\"},\"id\":\"55032953823\",\"timestamp\":\"2025-09-11 20:45:19.745\"}', 'completed', '2025-09-11 20:45:19', '2025-09-11 20:45:19');
INSERT INTO `task` VALUES (104, 'v9bmdm3', 'send_award', '91733838174', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"816010943288\",\"userId\":\"v9bmdm3\"},\"id\":\"91733838174\",\"timestamp\":\"2025-09-11 20:45:20.507\"}', 'completed', '2025-09-11 20:45:20', '2025-09-11 20:45:20');
INSERT INTO `task` VALUES (105, 'lm1md2_ggn63', 'send_award', '10273911981', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"846933349535\",\"userId\":\"lm1md2_ggn63\"},\"id\":\"10273911981\",\"timestamp\":\"2025-09-11 20:45:21.45\"}', 'completed', '2025-09-11 20:45:21', '2025-09-11 20:45:21');
INSERT INTO `task` VALUES (106, 'jwui2v10', 'send_award', '54823513524', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"634231351568\",\"userId\":\"jwui2v10\"},\"id\":\"54823513524\",\"timestamp\":\"2025-09-11 20:45:22.269\"}', 'completed', '2025-09-11 20:45:22', '2025-09-11 20:45:22');
INSERT INTO `task` VALUES (107, 'k1zk5m10', 'send_award', '83484535469', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"703936929763\",\"userId\":\"k1zk5m10\"},\"id\":\"83484535469\",\"timestamp\":\"2025-09-11 20:45:23.099\"}', 'completed', '2025-09-11 20:45:23', '2025-09-11 20:45:23');
INSERT INTO `task` VALUES (108, 'ttbtw9.j2h', 'send_award', '17774373181', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"002698604461\",\"userId\":\"ttbtw9.j2h\"},\"id\":\"17774373181\",\"timestamp\":\"2025-09-11 20:45:23.929\"}', 'completed', '2025-09-11 20:45:24', '2025-09-11 20:45:24');
INSERT INTO `task` VALUES (109, 'tw9izl.n4v31', 'send_award', '21305449562', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"972645872059\",\"userId\":\"tw9izl.n4v31\"},\"id\":\"21305449562\",\"timestamp\":\"2025-09-11 20:45:24.719\"}', 'completed', '2025-09-11 20:45:24', '2025-09-11 20:45:24');
INSERT INTO `task` VALUES (110, 'pu9ggq.lfs39', 'send_award', '27903391594', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"860121831868\",\"userId\":\"pu9ggq.lfs39\"},\"id\":\"27903391594\",\"timestamp\":\"2025-09-11 20:45:25.565\"}', 'completed', '2025-09-11 20:45:25', '2025-09-11 20:45:25');
INSERT INTO `task` VALUES (111, 'v9rmdm84', 'send_award', '68513984476', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"062273437164\",\"userId\":\"v9rmdm84\"},\"id\":\"68513984476\",\"timestamp\":\"2025-09-11 20:45:26.319\"}', 'completed', '2025-09-11 20:45:26', '2025-09-11 20:45:26');
INSERT INTO `task` VALUES (112, 'k4ti7b6', 'send_award', '90578321634', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"101706495049\",\"userId\":\"k4ti7b6\"},\"id\":\"90578321634\",\"timestamp\":\"2025-09-11 20:45:27.05\"}', 'completed', '2025-09-11 20:45:27', '2025-09-11 20:45:27');
INSERT INTO `task` VALUES (113, 'izhpd6_ffx11', 'send_award', '28766693252', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"261541472199\",\"userId\":\"izhpd6_ffx11\"},\"id\":\"28766693252\",\"timestamp\":\"2025-09-11 20:45:27.932\"}', 'completed', '2025-09-11 20:45:28', '2025-09-11 20:45:28');
INSERT INTO `task` VALUES (114, 'ttbtw9.i2vk1z', 'send_award', '07361096752', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"697345318013\",\"userId\":\"ttbtw9.i2vk1z\"},\"id\":\"07361096752\",\"timestamp\":\"2025-09-11 20:45:28.696\"}', 'completed', '2025-09-11 20:45:28', '2025-09-11 20:45:28');
INSERT INTO `task` VALUES (115, 'v9ru72.kdt97', 'send_award', '40259606487', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"662657061833\",\"userId\":\"v9ru72.kdt97\"},\"id\":\"40259606487\",\"timestamp\":\"2025-09-11 20:45:29.509\"}', 'completed', '2025-09-11 20:45:29', '2025-09-11 20:45:29');
INSERT INTO `task` VALUES (116, 'gncsbt.lel22', 'send_award', '91909119522', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"131711098135\",\"userId\":\"gncsbt.lel22\"},\"id\":\"91909119522\",\"timestamp\":\"2025-09-11 20:45:30.381\"}', 'completed', '2025-09-11 20:45:30', '2025-09-11 20:45:30');
INSERT INTO `task` VALUES (117, 'freg6n51', 'send_award', '99685992875', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"268258696805\",\"userId\":\"freg6n51\"},\"id\":\"99685992875\",\"timestamp\":\"2025-09-11 20:45:31.157\"}', 'completed', '2025-09-11 20:45:31', '2025-09-11 20:45:31');
INSERT INTO `task` VALUES (118, 'po4sf01', 'send_award', '23101830857', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"014550644150\",\"userId\":\"po4sf01\"},\"id\":\"23101830857\",\"timestamp\":\"2025-09-11 20:45:31.872\"}', 'completed', '2025-09-11 20:45:31', '2025-09-11 20:45:32');
INSERT INTO `task` VALUES (119, 'izhlq4.iyo', 'send_award', '88282887610', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"913494310498\",\"userId\":\"izhlq4.iyo\"},\"id\":\"88282887610\",\"timestamp\":\"2025-09-11 20:45:32.643\"}', 'completed', '2025-09-11 20:45:32', '2025-09-11 20:45:32');
INSERT INTO `task` VALUES (120, 'ffxlmf_o1p59', 'send_award', '55005649900', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"779407547422\",\"userId\":\"ffxlmf_o1p59\"},\"id\":\"55005649900\",\"timestamp\":\"2025-09-11 20:45:33.465\"}', 'completed', '2025-09-11 20:45:33', '2025-09-11 20:45:33');
INSERT INTO `task` VALUES (121, 'tbfv9r75', 'send_award', '39021472340', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"519359162157\",\"userId\":\"tbfv9r75\"},\"id\":\"39021472340\",\"timestamp\":\"2025-09-11 20:45:34.321\"}', 'completed', '2025-09-11 20:45:34', '2025-09-11 20:45:34');
INSERT INTO `task` VALUES (122, 'nlrlqi31', 'send_award', '06708115481', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"256041089293\",\"userId\":\"nlrlqi31\"},\"id\":\"06708115481\",\"timestamp\":\"2025-09-11 20:45:35.1\"}', 'completed', '2025-09-11 20:45:35', '2025-09-11 20:45:35');
INSERT INTO `task` VALUES (123, 'o97fmn35', 'send_award', '33375475917', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"822805218161\",\"userId\":\"o97fmn35\"},\"id\":\"33375475917\",\"timestamp\":\"2025-09-11 20:45:35.871\"}', 'completed', '2025-09-11 20:45:35', '2025-09-11 20:45:36');
INSERT INTO `task` VALUES (124, 'k7nggq_gc3', 'send_award', '19152220472', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"636417988062\",\"userId\":\"k7nggq_gc3\"},\"id\":\"19152220472\",\"timestamp\":\"2025-09-11 20:45:36.696\"}', 'completed', '2025-09-11 20:45:36', '2025-09-11 20:45:36');
INSERT INTO `task` VALUES (125, 'ucop8d83', 'send_award', '68699656217', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"232504766036\",\"userId\":\"ucop8d83\"},\"id\":\"68699656217\",\"timestamp\":\"2025-09-11 20:45:37.589\"}', 'completed', '2025-09-11 20:45:37', '2025-09-11 20:45:37');
INSERT INTO `task` VALUES (126, 'pu9ggq_tw253', 'send_award', '50077436459', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"908298497356\",\"userId\":\"pu9ggq_tw253\"},\"id\":\"50077436459\",\"timestamp\":\"2025-09-11 20:45:38.393\"}', 'completed', '2025-09-11 20:45:38', '2025-09-11 20:45:38');
INSERT INTO `task` VALUES (127, 'i2xmy2.u6m', 'send_award', '26245595694', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"000823273837\",\"userId\":\"i2xmy2.u6m\"},\"id\":\"26245595694\",\"timestamp\":\"2025-09-11 20:45:39.197\"}', 'completed', '2025-09-11 20:45:39', '2025-09-11 20:45:39');
INSERT INTO `task` VALUES (128, 'i1cvdk63', 'send_award', '23324373362', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"281965061322\",\"userId\":\"i1cvdk63\"},\"id\":\"23324373362\",\"timestamp\":\"2025-09-11 20:45:39.954\"}', 'completed', '2025-09-11 20:45:40', '2025-09-11 20:45:40');
INSERT INTO `task` VALUES (129, 'hm1nuy.gi196', 'send_award', '31054493959', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"657132214050\",\"userId\":\"hm1nuy.gi196\"},\"id\":\"31054493959\",\"timestamp\":\"2025-09-11 20:45:40.761\"}', 'completed', '2025-09-11 20:45:40', '2025-09-11 20:45:40');
INSERT INTO `task` VALUES (130, 'i1cl5v_fs1', 'send_award', '49485408488', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"279074109023\",\"userId\":\"i1cl5v_fs1\"},\"id\":\"49485408488\",\"timestamp\":\"2025-09-11 20:45:41.556\"}', 'completed', '2025-09-11 20:45:41', '2025-09-11 20:45:41');
INSERT INTO `task` VALUES (131, 'kgs.fh847', 'send_award', '21726183238', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"669646618798\",\"userId\":\"kgs.fh847\"},\"id\":\"21726183238\",\"timestamp\":\"2025-09-11 20:45:42.433\"}', 'completed', '2025-09-11 20:45:42', '2025-09-11 20:45:42');
INSERT INTO `task` VALUES (132, 'freg6n_k1z', 'send_award', '69910792356', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"021044035223\",\"userId\":\"freg6n_k1z\"},\"id\":\"69910792356\",\"timestamp\":\"2025-09-11 20:45:43.258\"}', 'completed', '2025-09-11 20:45:43', '2025-09-11 20:45:43');
INSERT INTO `task` VALUES (133, 'p72k3d.uc9', 'send_award', '40608974732', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"332375558446\",\"userId\":\"p72k3d.uc9\"},\"id\":\"40608974732\",\"timestamp\":\"2025-09-11 20:45:43.982\"}', 'completed', '2025-09-11 20:45:44', '2025-09-11 20:45:44');
INSERT INTO `task` VALUES (134, 'k1zk5m_n54', 'send_award', '00544547747', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"466909407992\",\"userId\":\"k1zk5m_n54\"},\"id\":\"00544547747\",\"timestamp\":\"2025-09-11 20:45:44.78\"}', 'completed', '2025-09-11 20:45:44', '2025-09-11 20:45:44');
INSERT INTO `task` VALUES (135, 'k18to3_p0z', 'send_award', '74683310958', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"264946207357\",\"userId\":\"k18to3_p0z\"},\"id\":\"74683310958\",\"timestamp\":\"2025-09-11 20:45:45.576\"}', 'completed', '2025-09-11 20:45:45', '2025-09-11 20:45:45');
INSERT INTO `task` VALUES (136, 'tbfv9r.lpvfim97', 'send_award', '55914859298', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"499739330647\",\"userId\":\"tbfv9r.lpvfim97\"},\"id\":\"55914859298\",\"timestamp\":\"2025-09-11 20:45:46.298\"}', 'completed', '2025-09-11 20:45:46', '2025-09-11 20:45:46');
INSERT INTO `task` VALUES (137, 'pu9ggq38', 'send_award', '67846186885', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"710323640113\",\"userId\":\"pu9ggq38\"},\"id\":\"67846186885\",\"timestamp\":\"2025-09-11 20:45:47.173\"}', 'completed', '2025-09-11 20:45:47', '2025-09-11 20:45:47');
INSERT INTO `task` VALUES (138, 'k8qlqi2', 'send_award', '38277194880', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"968533123052\",\"userId\":\"k8qlqi2\"},\"id\":\"38277194880\",\"timestamp\":\"2025-09-11 20:45:47.986\"}', 'completed', '2025-09-11 20:45:48', '2025-09-11 20:45:48');
INSERT INTO `task` VALUES (139, 'hm1i2v_kkh', 'send_award', '92297155719', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"727261148286\",\"userId\":\"hm1i2v_kkh\"},\"id\":\"92297155719\",\"timestamp\":\"2025-09-11 20:45:48.718\"}', 'completed', '2025-09-11 20:45:48', '2025-09-11 20:45:48');
INSERT INTO `task` VALUES (140, 'mawnuy_nh7', 'send_award', '54719542120', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"528496494899\",\"userId\":\"mawnuy_nh7\"},\"id\":\"54719542120\",\"timestamp\":\"2025-09-11 20:45:49.499\"}', 'completed', '2025-09-11 20:45:49', '2025-09-11 20:45:49');
INSERT INTO `task` VALUES (141, 'vacsbt43', 'send_award', '95475247357', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"089917255530\",\"userId\":\"vacsbt43\"},\"id\":\"95475247357\",\"timestamp\":\"2025-09-11 20:45:50.312\"}', 'completed', '2025-09-11 20:45:50', '2025-09-11 20:45:50');
INSERT INTO `task` VALUES (142, 'o97sbt16', 'send_award', '05088775586', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"681239720515\",\"userId\":\"o97sbt16\"},\"id\":\"05088775586\",\"timestamp\":\"2025-09-11 20:45:51.07\"}', 'completed', '2025-09-11 20:45:51', '2025-09-11 20:45:51');
INSERT INTO `task` VALUES (143, 'k88to374', 'send_award', '21257154777', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"235274431821\",\"userId\":\"k88to374\"},\"id\":\"21257154777\",\"timestamp\":\"2025-09-11 20:45:51.858\"}', 'completed', '2025-09-11 20:45:51', '2025-09-11 20:45:52');
INSERT INTO `task` VALUES (144, 'i1cvdk.k7f66', 'send_award', '59614086364', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"399069982632\",\"userId\":\"i1cvdk.k7f66\"},\"id\":\"59614086364\",\"timestamp\":\"2025-09-11 20:45:52.572\"}', 'completed', '2025-09-11 20:45:52', '2025-09-11 20:45:52');
INSERT INTO `task` VALUES (145, 'lmfmhh_rwe', 'send_award', '43309806093', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"292703443524\",\"userId\":\"lmfmhh_rwe\"},\"id\":\"43309806093\",\"timestamp\":\"2025-09-11 20:45:53.432\"}', 'completed', '2025-09-11 20:45:53', '2025-09-11 20:45:53');
INSERT INTO `task` VALUES (146, 'lmfmhh.k2j73', 'send_award', '85355078182', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"831687428725\",\"userId\":\"lmfmhh.k2j73\"},\"id\":\"85355078182\",\"timestamp\":\"2025-09-11 20:45:54.227\"}', 'completed', '2025-09-11 20:45:54', '2025-09-11 20:45:54');
INSERT INTO `task` VALUES (147, 'ob5j6i.ik1', 'send_award', '67877948312', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"882857075127\",\"userId\":\"ob5j6i.ik1\"},\"id\":\"67877948312\",\"timestamp\":\"2025-09-11 20:45:55.056\"}', 'completed', '2025-09-11 20:45:55', '2025-09-11 20:45:55');
INSERT INTO `task` VALUES (148, 'hm1i2v66', 'send_award', '14551834438', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"566605609703\",\"userId\":\"hm1i2v66\"},\"id\":\"14551834438\",\"timestamp\":\"2025-09-11 20:45:55.867\"}', 'completed', '2025-09-11 20:45:55', '2025-09-11 20:45:56');
INSERT INTO `task` VALUES (149, 'is8k1z_pvp32', 'send_award', '73534748940', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"663204164119\",\"userId\":\"is8k1z_pvp32\"},\"id\":\"73534748940\",\"timestamp\":\"2025-09-11 20:45:56.651\"}', 'completed', '2025-09-11 20:45:56', '2025-09-11 20:45:56');
INSERT INTO `task` VALUES (150, 'o97sbt64', 'send_award', '65868389177', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"754661956688\",\"userId\":\"o97sbt64\"},\"id\":\"65868389177\",\"timestamp\":\"2025-09-11 20:45:57.433\"}', 'completed', '2025-09-11 20:45:57', '2025-09-11 20:45:57');
INSERT INTO `task` VALUES (151, 'lzbmd2_u4x', 'send_award', '56957315460', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"168815378322\",\"userId\":\"lzbmd2_u4x\"},\"id\":\"56957315460\",\"timestamp\":\"2025-09-11 20:45:58.207\"}', 'completed', '2025-09-11 20:45:58', '2025-09-11 20:45:58');
INSERT INTO `task` VALUES (152, 'gs2k34.qx879', 'send_award', '63547604016', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"886237624534\",\"userId\":\"gs2k34.qx879\"},\"id\":\"63547604016\",\"timestamp\":\"2025-09-11 20:45:59.001\"}', 'completed', '2025-09-11 20:45:59', '2025-09-11 20:45:59');
INSERT INTO `task` VALUES (153, 'p1pscp87', 'send_award', '79485628514', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"225138803660\",\"userId\":\"p1pscp87\"},\"id\":\"79485628514\",\"timestamp\":\"2025-09-11 20:45:59.904\"}', 'completed', '2025-09-11 20:45:59', '2025-09-11 20:46:00');
INSERT INTO `task` VALUES (154, 'nlrlqi16', 'send_award', '45739873814', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"609335705023\",\"userId\":\"nlrlqi16\"},\"id\":\"45739873814\",\"timestamp\":\"2025-09-11 20:46:00.752\"}', 'completed', '2025-09-11 20:46:00', '2025-09-11 20:46:00');
INSERT INTO `task` VALUES (155, 'k5mit6.fke', 'send_award', '64709868193', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"521314442205\",\"userId\":\"k5mit6.fke\"},\"id\":\"64709868193\",\"timestamp\":\"2025-09-11 20:46:01.593\"}', 'completed', '2025-09-11 20:46:01', '2025-09-11 20:46:01');
INSERT INTO `task` VALUES (156, 'mawgge_uws', 'send_award', '58671074432', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"961520270278\",\"userId\":\"mawgge_uws\"},\"id\":\"58671074432\",\"timestamp\":\"2025-09-11 20:46:02.378\"}', 'completed', '2025-09-11 20:46:02', '2025-09-11 20:46:02');
INSERT INTO `task` VALUES (157, 'i1crqi.i78', 'send_award', '85991648080', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"943723262091\",\"userId\":\"i1crqi.i78\"},\"id\":\"85991648080\",\"timestamp\":\"2025-09-11 20:46:03.179\"}', 'completed', '2025-09-11 20:46:03', '2025-09-11 20:46:03');
INSERT INTO `task` VALUES (158, 'tgmo3v.l7p33', 'send_award', '23784258206', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"421460337531\",\"userId\":\"tgmo3v.l7p33\"},\"id\":\"23784258206\",\"timestamp\":\"2025-09-11 20:46:03.97\"}', 'completed', '2025-09-11 20:46:04', '2025-09-11 20:46:04');
INSERT INTO `task` VALUES (159, 'k7nggq23', 'send_award', '78125288868', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"077860360760\",\"userId\":\"k7nggq23\"},\"id\":\"78125288868\",\"timestamp\":\"2025-09-11 20:46:04.743\"}', 'completed', '2025-09-11 20:46:04', '2025-09-11 20:46:04');
INSERT INTO `task` VALUES (160, 'hm1nuy_n5c', 'send_award', '59657285602', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"736193297878\",\"userId\":\"hm1nuy_n5c\"},\"id\":\"59657285602\",\"timestamp\":\"2025-09-11 20:46:05.55\"}', 'completed', '2025-09-11 20:46:05', '2025-09-11 20:46:05');
INSERT INTO `task` VALUES (161, 'gs2m565', 'send_award', '79722032758', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"096775402100\",\"userId\":\"gs2m565\"},\"id\":\"79722032758\",\"timestamp\":\"2025-09-11 20:46:06.358\"}', 'completed', '2025-09-11 20:46:06', '2025-09-11 20:46:06');
INSERT INTO `task` VALUES (162, 'gs2m5617', 'send_award', '70158417750', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"019337974980\",\"userId\":\"gs2m5617\"},\"id\":\"70158417750\",\"timestamp\":\"2025-09-11 20:46:07.147\"}', 'completed', '2025-09-11 20:46:07', '2025-09-11 20:46:07');
INSERT INTO `task` VALUES (163, 'jcvsbt.glt95', 'send_award', '25684846403', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"727773414522\",\"userId\":\"jcvsbt.glt95\"},\"id\":\"25684846403\",\"timestamp\":\"2025-09-11 20:46:08.01\"}', 'completed', '2025-09-11 20:46:08', '2025-09-11 20:46:08');
INSERT INTO `task` VALUES (164, 'mg0iuc_nky', 'send_award', '94269833914', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"096264337911\",\"userId\":\"mg0iuc_nky\"},\"id\":\"94269833914\",\"timestamp\":\"2025-09-11 20:46:08.804\"}', 'completed', '2025-09-11 20:46:08', '2025-09-11 20:46:08');
INSERT INTO `task` VALUES (165, 'my230', 'send_award', '39640972195', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"440937137962\",\"userId\":\"my230\"},\"id\":\"39640972195\",\"timestamp\":\"2025-09-11 20:46:09.562\"}', 'completed', '2025-09-11 20:46:09', '2025-09-11 20:46:09');
INSERT INTO `task` VALUES (166, 'i1cvdk3', 'send_award', '72631845523', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"906392245153\",\"userId\":\"i1cvdk3\"},\"id\":\"72631845523\",\"timestamp\":\"2025-09-11 20:46:10.433\"}', 'completed', '2025-09-11 20:46:10', '2025-09-11 20:46:10');
INSERT INTO `task` VALUES (167, 'p1pscp68', 'send_award', '30076784569', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"211123330529\",\"userId\":\"p1pscp68\"},\"id\":\"30076784569\",\"timestamp\":\"2025-09-11 20:46:11.222\"}', 'completed', '2025-09-11 20:46:11', '2025-09-11 20:46:11');
INSERT INTO `task` VALUES (168, 'lnf59', 'send_award', '90678210439', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"018577031644\",\"userId\":\"lnf59\"},\"id\":\"90678210439\",\"timestamp\":\"2025-09-11 20:46:12.214\"}', 'completed', '2025-09-11 20:46:12', '2025-09-11 20:46:12');
INSERT INTO `task` VALUES (169, 'ffxlmf.puf58', 'send_award', '14712505144', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"201073428390\",\"userId\":\"ffxlmf.puf58\"},\"id\":\"14712505144\",\"timestamp\":\"2025-09-11 20:46:13.033\"}', 'completed', '2025-09-11 20:46:13', '2025-09-11 20:46:13');
INSERT INTO `task` VALUES (170, 'k5qsbt_o3y67', 'send_award', '15046200539', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"577835539649\",\"userId\":\"k5qsbt_o3y67\"},\"id\":\"15046200539\",\"timestamp\":\"2025-09-11 20:46:13.968\"}', 'completed', '2025-09-11 20:46:14', '2025-09-11 20:46:14');
INSERT INTO `task` VALUES (171, 'sf0ppm_gl296', 'send_award', '43478780693', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"983424474378\",\"userId\":\"sf0ppm_gl296\"},\"id\":\"43478780693\",\"timestamp\":\"2025-09-11 20:46:14.803\"}', 'completed', '2025-09-11 20:46:14', '2025-09-11 20:46:14');
INSERT INTO `task` VALUES (172, 'ggqryd.p8h32', 'send_award', '27495397826', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"638983123912\",\"userId\":\"ggqryd.p8h32\"},\"id\":\"27495397826\",\"timestamp\":\"2025-09-11 20:46:15.598\"}', 'completed', '2025-09-11 20:46:15', '2025-09-11 20:46:15');
INSERT INTO `task` VALUES (173, 'lm1sbt.tcu', 'send_award', '04224236799', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"955161245416\",\"userId\":\"lm1sbt.tcu\"},\"id\":\"04224236799\",\"timestamp\":\"2025-09-11 20:46:16.403\"}', 'completed', '2025-09-11 20:46:16', '2025-09-11 20:46:16');
INSERT INTO `task` VALUES (174, 'izhfoz.r5v', 'send_award', '69344902144', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"513263877014\",\"userId\":\"izhfoz.r5v\"},\"id\":\"69344902144\",\"timestamp\":\"2025-09-11 20:46:17.239\"}', 'completed', '2025-09-11 20:46:17', '2025-09-11 20:46:17');
INSERT INTO `task` VALUES (175, 'tw9izl_h9612', 'send_award', '50842292834', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"452489702941\",\"userId\":\"tw9izl_h9612\"},\"id\":\"50842292834\",\"timestamp\":\"2025-09-11 20:46:18.021\"}', 'completed', '2025-09-11 20:46:18', '2025-09-11 20:46:18');
INSERT INTO `task` VALUES (176, 'rmftt4.k0m', 'send_award', '29674291767', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"088093248433\",\"userId\":\"rmftt4.k0m\"},\"id\":\"29674291767\",\"timestamp\":\"2025-09-11 20:46:18.812\"}', 'completed', '2025-09-11 20:46:18', '2025-09-11 20:46:19');
INSERT INTO `task` VALUES (177, 'i1clo5_ny282', 'send_award', '99192839562', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"628279572336\",\"userId\":\"i1clo5_ny282\"},\"id\":\"99192839562\",\"timestamp\":\"2025-09-11 20:46:19.646\"}', 'completed', '2025-09-11 20:46:19', '2025-09-11 20:46:19');
INSERT INTO `task` VALUES (178, 'izhsf0.ka036', 'send_award', '82222921425', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"269441241708\",\"userId\":\"izhsf0.ka036\"},\"id\":\"82222921425\",\"timestamp\":\"2025-09-11 20:46:20.418\"}', 'completed', '2025-09-11 20:46:20', '2025-09-11 20:46:20');
INSERT INTO `task` VALUES (179, 'feog2w_tfa', 'send_award', '11104772099', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"162334120495\",\"userId\":\"feog2w_tfa\"},\"id\":\"11104772099\",\"timestamp\":\"2025-09-11 20:46:21.193\"}', 'completed', '2025-09-11 20:46:21', '2025-09-11 20:46:21');
INSERT INTO `task` VALUES (180, 'fvpts463', 'send_award', '97081607044', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"602546447780\",\"userId\":\"fvpts463\"},\"id\":\"97081607044\",\"timestamp\":\"2025-09-11 20:46:21.98\"}', 'completed', '2025-09-11 20:46:22', '2025-09-11 20:46:22');
INSERT INTO `task` VALUES (181, 'ktzmzi_h96', 'send_award', '63311890683', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"568965923397\",\"userId\":\"ktzmzi_h96\"},\"id\":\"63311890683\",\"timestamp\":\"2025-09-11 20:46:22.723\"}', 'completed', '2025-09-11 20:46:22', '2025-09-11 20:46:22');
INSERT INTO `task` VALUES (182, 'kgs.ojx', 'send_award', '88489399735', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"807637026925\",\"userId\":\"kgs.ojx\"},\"id\":\"88489399735\",\"timestamp\":\"2025-09-11 20:46:23.525\"}', 'completed', '2025-09-11 20:46:23', '2025-09-11 20:46:23');
INSERT INTO `task` VALUES (183, 'o97kgs_rw844', 'send_award', '74764325367', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"967736944185\",\"userId\":\"o97kgs_rw844\"},\"id\":\"74764325367\",\"timestamp\":\"2025-09-11 20:46:24.33\"}', 'completed', '2025-09-11 20:46:24', '2025-09-11 20:46:24');
INSERT INTO `task` VALUES (184, 'fvpts4_om789', 'send_award', '70852311380', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"190758392449\",\"userId\":\"fvpts4_om789\"},\"id\":\"70852311380\",\"timestamp\":\"2025-09-11 20:46:25.174\"}', 'completed', '2025-09-11 20:46:25', '2025-09-11 20:46:25');
INSERT INTO `task` VALUES (185, 'tt6kfy.oq372', 'send_award', '08519621654', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"244152113303\",\"userId\":\"tt6kfy.oq372\"},\"id\":\"08519621654\",\"timestamp\":\"2025-09-11 20:46:25.949\"}', 'completed', '2025-09-11 20:46:26', '2025-09-11 20:46:26');
INSERT INTO `task` VALUES (186, 'ggqlnf_fo574', 'send_award', '57734242351', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"034820604596\",\"userId\":\"ggqlnf_fo574\"},\"id\":\"57734242351\",\"timestamp\":\"2025-09-11 20:46:26.721\"}', 'completed', '2025-09-11 20:46:26', '2025-09-11 20:46:26');
INSERT INTO `task` VALUES (187, 'i1clo517', 'send_award', '68153071176', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"274326223439\",\"userId\":\"i1clo517\"},\"id\":\"68153071176\",\"timestamp\":\"2025-09-11 20:46:27.54\"}', 'completed', '2025-09-11 20:46:27', '2025-09-11 20:46:27');
INSERT INTO `task` VALUES (188, 'lm1sds_gii61', 'send_award', '73443325963', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"888275641024\",\"userId\":\"lm1sds_gii61\"},\"id\":\"73443325963\",\"timestamp\":\"2025-09-11 20:46:28.387\"}', 'completed', '2025-09-11 20:46:28', '2025-09-11 20:46:28');
INSERT INTO `task` VALUES (189, 'pu9ggq_s3t71', 'send_award', '37840194315', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"788059486915\",\"userId\":\"pu9ggq_s3t71\"},\"id\":\"37840194315\",\"timestamp\":\"2025-09-11 20:46:29.227\"}', 'completed', '2025-09-11 20:46:29', '2025-09-11 20:46:29');
INSERT INTO `task` VALUES (190, 'tbfv9r91', 'send_award', '11451080232', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"755595728593\",\"userId\":\"tbfv9r91\"},\"id\":\"11451080232\",\"timestamp\":\"2025-09-11 20:46:30.037\"}', 'completed', '2025-09-11 20:46:30', '2025-09-11 20:46:30');
INSERT INTO `task` VALUES (191, 'gncsbt25', 'send_award', '82756354885', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"049427887063\",\"userId\":\"gncsbt25\"},\"id\":\"82756354885\",\"timestamp\":\"2025-09-11 20:46:30.914\"}', 'completed', '2025-09-11 20:46:31', '2025-09-11 20:46:31');
INSERT INTO `task` VALUES (192, 'gncsbt51', 'send_award', '49849734347', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"720279080918\",\"userId\":\"gncsbt51\"},\"id\":\"49849734347\",\"timestamp\":\"2025-09-11 20:46:31.757\"}', 'completed', '2025-09-11 20:46:31', '2025-09-11 20:46:31');
INSERT INTO `task` VALUES (193, 'hm1p8q.mtn', 'send_award', '69796817209', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"899656495241\",\"userId\":\"hm1p8q.mtn\"},\"id\":\"69796817209\",\"timestamp\":\"2025-09-11 20:46:32.595\"}', 'completed', '2025-09-11 20:46:32', '2025-09-11 20:46:32');
INSERT INTO `task` VALUES (194, 'tgmo3v.ptd0', 'send_award', '75817773989', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"874913292666\",\"userId\":\"tgmo3v.ptd0\"},\"id\":\"75817773989\",\"timestamp\":\"2025-09-11 20:46:33.357\"}', 'completed', '2025-09-11 20:46:33', '2025-09-11 20:46:33');
INSERT INTO `task` VALUES (195, 'tbfnuy.fof31', 'send_award', '32395774989', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"145467791412\",\"userId\":\"tbfnuy.fof31\"},\"id\":\"32395774989\",\"timestamp\":\"2025-09-11 20:46:34.096\"}', 'completed', '2025-09-11 20:46:34', '2025-09-11 20:46:34');
INSERT INTO `task` VALUES (196, 'ob5j6i.vcr10', 'send_award', '56479161513', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"703373154788\",\"userId\":\"ob5j6i.vcr10\"},\"id\":\"56479161513\",\"timestamp\":\"2025-09-11 20:46:34.918\"}', 'completed', '2025-09-11 20:46:35', '2025-09-11 20:46:35');
INSERT INTO `task` VALUES (197, 'jlbi46.g30i1l', 'send_award', '07946196903', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"040363070521\",\"userId\":\"jlbi46.g30i1l\"},\"id\":\"07946196903\",\"timestamp\":\"2025-09-11 20:46:35.726\"}', 'completed', '2025-09-11 20:46:35', '2025-09-11 20:46:35');
INSERT INTO `task` VALUES (198, 'gs2k34.fgw', 'send_award', '43525110999', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"789544422535\",\"userId\":\"gs2k34.fgw\"},\"id\":\"43525110999\",\"timestamp\":\"2025-09-11 20:46:36.548\"}', 'completed', '2025-09-11 20:46:36', '2025-09-11 20:46:36');
INSERT INTO `task` VALUES (199, 'k1zu0c.fgs', 'send_award', '10992695600', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"164211006185\",\"userId\":\"k1zu0c.fgs\"},\"id\":\"10992695600\",\"timestamp\":\"2025-09-11 20:46:37.366\"}', 'completed', '2025-09-11 20:46:37', '2025-09-11 20:46:37');
INSERT INTO `task` VALUES (200, 'v9rlnf92', 'send_award', '55971184927', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"373505928385\",\"userId\":\"v9rlnf92\"},\"id\":\"55971184927\",\"timestamp\":\"2025-09-11 20:46:38.169\"}', 'completed', '2025-09-11 20:46:38', '2025-09-11 20:46:38');
INSERT INTO `task` VALUES (201, 'mawgge_itu', 'send_award', '88600048608', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"502767082134\",\"userId\":\"mawgge_itu\"},\"id\":\"88600048608\",\"timestamp\":\"2025-09-11 20:46:38.983\"}', 'completed', '2025-09-11 20:46:39', '2025-09-11 20:46:39');
INSERT INTO `task` VALUES (202, 'k5qgs270', 'send_award', '82464857142', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"163734336600\",\"userId\":\"k5qgs270\"},\"id\":\"82464857142\",\"timestamp\":\"2025-09-11 20:46:39.795\"}', 'completed', '2025-09-11 20:46:39', '2025-09-11 20:46:40');
INSERT INTO `task` VALUES (203, 'jlbffg15', 'send_award', '18174171997', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"052819646350\",\"userId\":\"jlbffg15\"},\"id\":\"18174171997\",\"timestamp\":\"2025-09-11 20:46:40.688\"}', 'completed', '2025-09-11 20:46:40', '2025-09-11 20:46:40');
INSERT INTO `task` VALUES (204, 'k4ti7b21', 'send_award', '32132630444', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"287976874025\",\"userId\":\"k4ti7b21\"},\"id\":\"32132630444\",\"timestamp\":\"2025-09-11 20:46:41.495\"}', 'completed', '2025-09-11 20:46:41', '2025-09-11 20:46:41');
INSERT INTO `task` VALUES (205, 'uclgm3.nh4', 'send_award', '97704526192', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"700913857836\",\"userId\":\"uclgm3.nh4\"},\"id\":\"97704526192\",\"timestamp\":\"2025-09-11 20:46:42.352\"}', 'completed', '2025-09-11 20:46:42', '2025-09-11 20:46:42');
INSERT INTO `task` VALUES (206, 'v9ru7274', 'send_award', '61818071242', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"224493992388\",\"userId\":\"v9ru7274\"},\"id\":\"61818071242\",\"timestamp\":\"2025-09-11 20:46:43.12\"}', 'completed', '2025-09-11 20:46:43', '2025-09-11 20:46:43');
INSERT INTO `task` VALUES (207, 'uclgm3_hs4', 'send_award', '52746483449', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"755330709430\",\"userId\":\"uclgm3_hs4\"},\"id\":\"52746483449\",\"timestamp\":\"2025-09-11 20:46:43.93\"}', 'completed', '2025-09-11 20:46:44', '2025-09-11 20:46:44');
INSERT INTO `task` VALUES (208, 'v9rmdm.ui4', 'send_award', '04968319579', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"710456864745\",\"userId\":\"v9rmdm.ui4\"},\"id\":\"04968319579\",\"timestamp\":\"2025-09-11 20:46:44.816\"}', 'completed', '2025-09-11 20:46:44', '2025-09-11 20:46:44');
INSERT INTO `task` VALUES (209, 'rydkjv_kjv91', 'send_award', '29641710864', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"701788435796\",\"userId\":\"rydkjv_kjv91\"},\"id\":\"29641710864\",\"timestamp\":\"2025-09-11 20:46:45.65\"}', 'completed', '2025-09-11 20:46:45', '2025-09-11 20:46:45');
INSERT INTO `task` VALUES (210, 'k7znlr38', 'send_award', '93152035491', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"761270124687\",\"userId\":\"k7znlr38\"},\"id\":\"93152035491\",\"timestamp\":\"2025-09-11 20:46:46.472\"}', 'completed', '2025-09-11 20:46:46', '2025-09-11 20:46:46');
INSERT INTO `task` VALUES (211, 'p1pscp_fo50', 'send_award', '35922884683', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"804330544240\",\"userId\":\"p1pscp_fo50\"},\"id\":\"35922884683\",\"timestamp\":\"2025-09-11 20:46:47.222\"}', 'completed', '2025-09-11 20:46:47', '2025-09-11 20:46:47');
INSERT INTO `task` VALUES (212, 'irescp.lel27', 'send_award', '40945593610', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"496349760622\",\"userId\":\"irescp.lel27\"},\"id\":\"40945593610\",\"timestamp\":\"2025-09-11 20:46:48.118\"}', 'completed', '2025-09-11 20:46:48', '2025-09-11 20:46:48');
INSERT INTO `task` VALUES (213, 'ts5tbf43', 'send_award', '40166325744', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"848741623647\",\"userId\":\"ts5tbf43\"},\"id\":\"40166325744\",\"timestamp\":\"2025-09-11 20:46:48.888\"}', 'completed', '2025-09-11 20:46:48', '2025-09-11 20:46:49');
INSERT INTO `task` VALUES (214, 'my250', 'send_award', '06933107612', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"095855023627\",\"userId\":\"my250\"},\"id\":\"06933107612\",\"timestamp\":\"2025-09-11 20:46:49.645\"}', 'completed', '2025-09-11 20:46:49', '2025-09-11 20:46:49');
INSERT INTO `task` VALUES (215, 'freui8.kgq', 'send_award', '50030886635', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"828040489556\",\"userId\":\"freui8.kgq\"},\"id\":\"50030886635\",\"timestamp\":\"2025-09-11 20:46:50.47\"}', 'completed', '2025-09-11 20:46:50', '2025-09-11 20:46:50');
INSERT INTO `task` VALUES (216, 'lm1_sny', 'send_award', '11981201031', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"227357598813\",\"userId\":\"lm1_sny\"},\"id\":\"11981201031\",\"timestamp\":\"2025-09-11 20:46:51.249\"}', 'completed', '2025-09-11 20:46:51', '2025-09-11 20:46:51');
INSERT INTO `task` VALUES (217, 'jlbffg.nky48', 'send_award', '52189157265', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"576425970003\",\"userId\":\"jlbffg.nky48\"},\"id\":\"52189157265\",\"timestamp\":\"2025-09-11 20:46:52.055\"}', 'completed', '2025-09-11 20:46:52', '2025-09-11 20:46:52');
INSERT INTO `task` VALUES (218, 'mawfmn_tm050', 'send_award', '43599373417', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"604453012860\",\"userId\":\"mawfmn_tm050\"},\"id\":\"43599373417\",\"timestamp\":\"2025-09-11 20:46:52.826\"}', 'completed', '2025-09-11 20:46:53', '2025-09-11 20:46:53');
INSERT INTO `task` VALUES (219, 'fmnrmi35', 'send_award', '63963231179', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"930968326245\",\"userId\":\"fmnrmi35\"},\"id\":\"63963231179\",\"timestamp\":\"2025-09-11 20:46:54.401\"}', 'completed', '2025-09-11 20:46:54', '2025-09-11 20:46:54');
INSERT INTO `task` VALUES (220, 'k7znlr_go8', 'send_award', '58347093838', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"725174156490\",\"userId\":\"k7znlr_go8\"},\"id\":\"58347093838\",\"timestamp\":\"2025-09-11 20:46:55.151\"}', 'completed', '2025-09-11 20:46:55', '2025-09-11 20:46:55');
INSERT INTO `task` VALUES (221, 'k8qlqi.lld83', 'send_award', '21027211852', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"683129960506\",\"userId\":\"k8qlqi.lld83\"},\"id\":\"21027211852\",\"timestamp\":\"2025-09-11 20:46:55.951\"}', 'completed', '2025-09-11 20:46:56', '2025-09-11 20:46:56');
INSERT INTO `task` VALUES (222, 'k5qgs271', 'send_award', '42807246764', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"091079022426\",\"userId\":\"k5qgs271\"},\"id\":\"42807246764\",\"timestamp\":\"2025-09-11 20:46:56.771\"}', 'completed', '2025-09-11 20:46:56', '2025-09-11 20:46:56');
INSERT INTO `task` VALUES (223, 'k5mmd2.fg017', 'send_award', '14739855164', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"542194017491\",\"userId\":\"k5mmd2.fg017\"},\"id\":\"14739855164\",\"timestamp\":\"2025-09-11 20:46:57.587\"}', 'completed', '2025-09-11 20:46:57', '2025-09-11 20:46:57');
INSERT INTO `task` VALUES (224, 'mg0iuc26', 'send_award', '71205468425', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"564280232468\",\"userId\":\"mg0iuc26\"},\"id\":\"71205468425\",\"timestamp\":\"2025-09-11 20:46:58.406\"}', 'completed', '2025-09-11 20:46:58', '2025-09-11 20:46:58');
INSERT INTO `task` VALUES (225, 'k8qkfk82', 'send_award', '42277375296', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"727847101472\",\"userId\":\"k8qkfk82\"},\"id\":\"42277375296\",\"timestamp\":\"2025-09-11 20:46:59.213\"}', 'completed', '2025-09-11 20:46:59', '2025-09-11 20:46:59');
INSERT INTO `task` VALUES (226, 'l7nrqi26', 'send_award', '97825119653', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"735090489834\",\"userId\":\"l7nrqi26\"},\"id\":\"97825119653\",\"timestamp\":\"2025-09-11 20:46:59.993\"}', 'completed', '2025-09-11 20:47:00', '2025-09-11 20:47:00');
INSERT INTO `task` VALUES (227, 'ttbtw9_k6731', 'send_award', '26350993746', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"508237237670\",\"userId\":\"ttbtw9_k6731\"},\"id\":\"26350993746\",\"timestamp\":\"2025-09-11 20:47:00.831\"}', 'completed', '2025-09-11 20:47:00', '2025-09-11 20:47:00');
INSERT INTO `task` VALUES (228, 'irescp_gl4o6y', 'send_award', '69073615263', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"454556556132\",\"userId\":\"irescp_gl4o6y\"},\"id\":\"69073615263\",\"timestamp\":\"2025-09-11 20:47:01.64\"}', 'completed', '2025-09-11 20:47:01', '2025-09-11 20:47:01');
INSERT INTO `task` VALUES (229, 'ob5j6i_nevn5742', 'send_award', '60728476635', '{\"data\":{\"awardConfig\":\"\",\"awardId\":103,\"awardTitle\":\"支付优惠券\",\"orderId\":\"223186507909\",\"userId\":\"ob5j6i_nevn5742\"},\"id\":\"60728476635\",\"timestamp\":\"2025-09-11 20:47:02.425\"}', 'completed', '2025-09-11 20:47:02', '2025-09-11 20:47:02');
INSERT INTO `task` VALUES (230, 'fmni4885', 'send_award', '39023604625', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"014212327196\",\"userId\":\"fmni4885\"},\"id\":\"39023604625\",\"timestamp\":\"2025-09-11 20:47:03.173\"}', 'completed', '2025-09-11 20:47:03', '2025-09-11 20:47:03');
INSERT INTO `task` VALUES (231, 'ppm_g3a46', 'send_award', '66027766149', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"909087386821\",\"userId\":\"ppm_g3a46\"},\"id\":\"66027766149\",\"timestamp\":\"2025-09-11 20:47:03.996\"}', 'completed', '2025-09-11 20:47:04', '2025-09-11 20:47:04');
INSERT INTO `task` VALUES (232, 'fevfoz96', 'send_award', '80321878591', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"192333842642\",\"userId\":\"fevfoz96\"},\"id\":\"80321878591\",\"timestamp\":\"2025-09-11 20:47:04.843\"}', 'completed', '2025-09-11 20:47:04', '2025-09-11 20:47:05');
INSERT INTO `task` VALUES (233, 'tw6my2.roi', 'send_award', '88737877396', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"822355996854\",\"userId\":\"tw6my2.roi\"},\"id\":\"88737877396\",\"timestamp\":\"2025-09-11 20:47:05.719\"}', 'completed', '2025-09-11 20:47:05', '2025-09-11 20:47:05');
INSERT INTO `task` VALUES (234, 'v9rmdm_fevfrz12', 'send_award', '33216537730', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"225486083838\",\"userId\":\"v9rmdm_fevfrz12\"},\"id\":\"33216537730\",\"timestamp\":\"2025-09-11 20:47:06.5\"}', 'completed', '2025-09-11 20:47:06', '2025-09-11 20:47:06');
INSERT INTO `task` VALUES (235, 'k7znlr48', 'send_award', '86159478765', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"909759882751\",\"userId\":\"k7znlr48\"},\"id\":\"86159478765\",\"timestamp\":\"2025-09-11 20:47:07.296\"}', 'completed', '2025-09-11 20:47:07', '2025-09-11 20:47:07');
INSERT INTO `task` VALUES (236, 'ggqryd.twz81', 'send_award', '55147334625', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"245589492695\",\"userId\":\"ggqryd.twz81\"},\"id\":\"55147334625\",\"timestamp\":\"2025-09-11 20:47:08.103\"}', 'completed', '2025-09-11 20:47:08', '2025-09-11 20:47:08');
INSERT INTO `task` VALUES (237, 'n0emzg_tog', 'send_award', '51808301956', '{\"data\":{\"awardConfig\":\"\",\"awardId\":107,\"awardTitle\":\"小霸王游戏机\",\"orderId\":\"576059853319\",\"userId\":\"n0emzg_tog\"},\"id\":\"51808301956\",\"timestamp\":\"2025-09-11 20:47:08.901\"}', 'completed', '2025-09-11 20:47:08', '2025-09-11 20:47:09');
INSERT INTO `task` VALUES (238, 'o97rmi.gbf98', 'send_award', '44745270713', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"935817324091\",\"userId\":\"o97rmi.gbf98\"},\"id\":\"44745270713\",\"timestamp\":\"2025-09-11 20:47:09.681\"}', 'completed', '2025-09-11 20:47:09', '2025-09-11 20:47:09');
INSERT INTO `task` VALUES (239, 'pd6fvp_gky51', 'send_award', '07390336907', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"029694653747\",\"userId\":\"pd6fvp_gky51\"},\"id\":\"07390336907\",\"timestamp\":\"2025-09-11 20:47:10.459\"}', 'completed', '2025-09-11 20:47:10', '2025-09-11 20:47:10');
INSERT INTO `task` VALUES (240, 'p72k3d_qjm', 'send_award', '66590504153', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"930684869207\",\"userId\":\"p72k3d_qjm\"},\"id\":\"66590504153\",\"timestamp\":\"2025-09-11 20:47:11.31\"}', 'completed', '2025-09-11 20:47:11', '2025-09-11 20:47:11');
INSERT INTO `task` VALUES (241, 'me4hce.sld', 'send_award', '60641689134', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"636778115994\",\"userId\":\"me4hce.sld\"},\"id\":\"60641689134\",\"timestamp\":\"2025-09-11 20:47:12.082\"}', 'completed', '2025-09-11 20:47:12', '2025-09-11 20:47:12');
INSERT INTO `task` VALUES (242, 'fmnrmi_vcr', 'send_award', '63043438819', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"510999145303\",\"userId\":\"fmnrmi_vcr\"},\"id\":\"63043438819\",\"timestamp\":\"2025-09-11 20:47:12.896\"}', 'completed', '2025-09-11 20:47:12', '2025-09-11 20:47:13');
INSERT INTO `task` VALUES (243, 'tt6kfy_nyg', 'send_award', '64962739911', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"974224320960\",\"userId\":\"tt6kfy_nyg\"},\"id\":\"64962739911\",\"timestamp\":\"2025-09-11 20:47:13.63\"}', 'completed', '2025-09-11 20:47:13', '2025-09-11 20:47:13');
INSERT INTO `task` VALUES (244, 'fozmy2_o9x53', 'send_award', '47548621768', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"058878789987\",\"userId\":\"fozmy2_o9x53\"},\"id\":\"47548621768\",\"timestamp\":\"2025-09-11 20:47:14.443\"}', 'completed', '2025-09-11 20:47:14', '2025-09-11 20:47:14');
INSERT INTO `task` VALUES (245, 'pd6fvp_shb', 'send_award', '13107037155', '{\"data\":{\"awardConfig\":\"\",\"awardId\":104,\"awardTitle\":\"小米台灯\",\"orderId\":\"414109579844\",\"userId\":\"pd6fvp_shb\"},\"id\":\"13107037155\",\"timestamp\":\"2025-09-11 20:47:15.212\"}', 'completed', '2025-09-11 20:47:15', '2025-09-11 20:47:15');
INSERT INTO `task` VALUES (246, 'gs2k345', 'send_award', '40668282318', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"006237162453\",\"userId\":\"gs2k345\"},\"id\":\"40668282318\",\"timestamp\":\"2025-09-11 20:47:16.011\"}', 'completed', '2025-09-11 20:47:16', '2025-09-11 20:47:16');
INSERT INTO `task` VALUES (247, 'izhlq416', 'send_award', '52308386696', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"919634110683\",\"userId\":\"izhlq416\"},\"id\":\"52308386696\",\"timestamp\":\"2025-09-11 20:47:16.866\"}', 'completed', '2025-09-11 20:47:16', '2025-09-11 20:47:17');
INSERT INTO `task` VALUES (248, 'fmnlj154', 'send_award', '99440031869', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"889895918543\",\"userId\":\"fmnlj154\"},\"id\":\"99440031869\",\"timestamp\":\"2025-09-11 20:47:17.616\"}', 'completed', '2025-09-11 20:47:17', '2025-09-11 20:47:17');
INSERT INTO `task` VALUES (249, 'tbfv9r.ul4', 'send_award', '83812627192', '{\"data\":{\"awardConfig\":\"\",\"awardId\":108,\"awardTitle\":\"暴走玩偶\",\"orderId\":\"511366015638\",\"userId\":\"tbfv9r.ul4\"},\"id\":\"83812627192\",\"timestamp\":\"2025-09-11 20:47:18.411\"}', 'completed', '2025-09-11 20:47:18', '2025-09-11 20:47:18');
INSERT INTO `task` VALUES (250, 'fextfv63', 'send_award', '79396454542', '{\"data\":{\"awardConfig\":\"\",\"awardId\":102,\"awardTitle\":\"OpenAI会员卡\",\"orderId\":\"323297045715\",\"userId\":\"fextfv63\"},\"id\":\"79396454542\",\"timestamp\":\"2025-09-11 20:47:19.239\"}', 'completed', '2025-09-11 20:47:19', '2025-09-11 20:47:19');
INSERT INTO `task` VALUES (251, 'lm182', 'send_award', '91450987305', '{\"data\":{\"awardConfig\":\"1,100\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"329980206213\",\"userId\":\"lm182\"},\"id\":\"91450987305\",\"timestamp\":\"2025-09-11 20:47:20.056\"}', 'completed', '2025-09-11 20:47:20', '2025-09-11 20:47:20');
INSERT INTO `task` VALUES (252, 'jlbffg92', 'send_award', '92469829528', '{\"data\":{\"awardConfig\":\"\",\"awardId\":101,\"awardTitle\":\"随机积分\",\"orderId\":\"517098628988\",\"userId\":\"jlbffg92\"},\"id\":\"92469829528\",\"timestamp\":\"2025-09-11 20:47:20.923\"}', 'completed', '2025-09-11 20:47:21', '2025-09-11 20:47:21');

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
INSERT INTO `user_award_record_000` VALUES (1, 'ppm52', 100301, 100006, '089122622827', 101, '随机积分', '2025-09-11 12:43:58', 'completed', '2025-09-11 20:43:58', '2025-09-11 20:43:58');
INSERT INTO `user_award_record_000` VALUES (2, 'izhlq4_gii', 100301, 100006, '937148513755', 101, '随机积分', '2025-09-11 12:44:01', 'completed', '2025-09-11 20:44:00', '2025-09-11 20:44:00');
INSERT INTO `user_award_record_000` VALUES (3, 'jzzgm357', 100301, 100006, '285290209479', 104, '小米台灯', '2025-09-11 12:44:01', 'create', '2025-09-11 20:44:01', '2025-09-11 20:44:01');
INSERT INTO `user_award_record_000` VALUES (4, 'izhlq4.rxn', 100301, 100006, '392717571649', 102, 'OpenAI会员卡', '2025-09-11 12:44:05', 'create', '2025-09-11 20:44:05', '2025-09-11 20:44:05');
INSERT INTO `user_award_record_000` VALUES (5, 'ggqlnf_tca', 100301, 100006, '363170555865', 108, '暴走玩偶', '2025-09-11 12:44:08', 'create', '2025-09-11 20:44:07', '2025-09-11 20:44:07');
INSERT INTO `user_award_record_000` VALUES (6, 'k8qi488', 100301, 100006, '989817973678', 107, '小霸王游戏机', '2025-09-11 12:44:13', 'create', '2025-09-11 20:44:13', '2025-09-11 20:44:13');
INSERT INTO `user_award_record_000` VALUES (7, 'lm1sbt_seo', 100301, 100006, '060715855252', 101, '随机积分', '2025-09-11 12:44:15', 'completed', '2025-09-11 20:44:14', '2025-09-11 20:44:15');
INSERT INTO `user_award_record_000` VALUES (8, 'k8qscp8', 100301, 100006, '353860910031', 104, '小米台灯', '2025-09-11 12:44:17', 'create', '2025-09-11 20:44:16', '2025-09-11 20:44:16');
INSERT INTO `user_award_record_000` VALUES (9, 'izh38', 100301, 100006, '130835750784', 104, '小米台灯', '2025-09-11 12:44:19', 'create', '2025-09-11 20:44:18', '2025-09-11 20:44:18');
INSERT INTO `user_award_record_000` VALUES (10, 'k1zggq8', 100301, 100006, '139380037709', 102, 'OpenAI会员卡', '2025-09-11 12:44:26', 'create', '2025-09-11 20:44:25', '2025-09-11 20:44:25');
INSERT INTO `user_award_record_000` VALUES (11, 'ucop8d_skb26', 100301, 100006, '847493615368', 101, '随机积分', '2025-09-11 12:44:30', 'completed', '2025-09-11 20:44:29', '2025-09-11 20:44:29');
INSERT INTO `user_award_record_000` VALUES (12, 'jzzgm3_kls', 100301, 100006, '601722234977', 101, '随机积分', '2025-09-11 12:44:32', 'completed', '2025-09-11 20:44:32', '2025-09-11 20:44:32');
INSERT INTO `user_award_record_000` VALUES (13, 'hm1p8q25', 100301, 100006, '695015150623', 101, '随机积分', '2025-09-11 12:44:34', 'completed', '2025-09-11 20:44:33', '2025-09-11 20:44:33');
INSERT INTO `user_award_record_000` VALUES (14, 'mawtue.tfa', 100301, 100006, '761928899734', 102, 'OpenAI会员卡', '2025-09-11 12:44:43', 'create', '2025-09-11 20:44:42', '2025-09-11 20:44:42');
INSERT INTO `user_award_record_000` VALUES (15, 'lh7lnf81', 100301, 100006, '047229855366', 101, '随机积分', '2025-09-11 12:44:49', 'completed', '2025-09-11 20:44:49', '2025-09-11 20:44:49');
INSERT INTO `user_award_record_000` VALUES (16, 'ixjit6.ldu80', 100301, 100006, '278057521875', 103, '支付优惠券', '2025-09-11 12:44:50', 'create', '2025-09-11 20:44:50', '2025-09-11 20:44:50');
INSERT INTO `user_award_record_000` VALUES (17, 'ggqryd.rwe1', 100301, 100006, '080027856118', 101, '随机积分', '2025-09-11 12:44:57', 'completed', '2025-09-11 20:44:56', '2025-09-11 20:44:56');
INSERT INTO `user_award_record_000` VALUES (18, 'kgs.i60', 100301, 100006, '896999071131', 104, '小米台灯', '2025-09-11 12:45:01', 'create', '2025-09-11 20:45:01', '2025-09-11 20:45:01');
INSERT INTO `user_award_record_000` VALUES (19, 'k7znlr46', 100301, 100006, '539505157720', 101, '随机积分', '2025-09-11 12:45:02', 'completed', '2025-09-11 20:45:02', '2025-09-11 20:45:02');
INSERT INTO `user_award_record_000` VALUES (20, 'p1pvgw.fkp22', 100301, 100006, '927384341281', 101, '随机积分', '2025-09-11 12:45:04', 'completed', '2025-09-11 20:45:03', '2025-09-11 20:45:03');
INSERT INTO `user_award_record_000` VALUES (21, 'i1csbt70', 100301, 100006, '561507325512', 103, '支付优惠券', '2025-09-11 12:45:04', 'create', '2025-09-11 20:45:04', '2025-09-11 20:45:04');
INSERT INTO `user_award_record_000` VALUES (22, 'lh7lnf.klt77', 100301, 100006, '059644126934', 104, '小米台灯', '2025-09-11 12:45:09', 'create', '2025-09-11 20:45:09', '2025-09-11 20:45:09');
INSERT INTO `user_award_record_000` VALUES (23, 'jwui2v_jjl', 100301, 100006, '585060132129', 108, '暴走玩偶', '2025-09-11 12:45:10', 'create', '2025-09-11 20:45:10', '2025-09-11 20:45:10');
INSERT INTO `user_award_record_000` VALUES (24, 'ttbtw9.j2h', 100301, 100006, '002698604461', 107, '小霸王游戏机', '2025-09-11 12:45:24', 'create', '2025-09-11 20:45:24', '2025-09-11 20:45:24');
INSERT INTO `user_award_record_000` VALUES (25, 'izhpd6_ffx11', 100301, 100006, '261541472199', 102, 'OpenAI会员卡', '2025-09-11 12:45:28', 'create', '2025-09-11 20:45:28', '2025-09-11 20:45:28');
INSERT INTO `user_award_record_000` VALUES (26, 'o97fmn35', 100301, 100006, '822805218161', 108, '暴走玩偶', '2025-09-11 12:45:36', 'create', '2025-09-11 20:45:35', '2025-09-11 20:45:35');
INSERT INTO `user_award_record_000` VALUES (27, 'pu9ggq_tw253', 100301, 100006, '908298497356', 102, 'OpenAI会员卡', '2025-09-11 12:45:38', 'create', '2025-09-11 20:45:38', '2025-09-11 20:45:38');
INSERT INTO `user_award_record_000` VALUES (28, 'hm1nuy.gi196', 100301, 100006, '657132214050', 108, '暴走玩偶', '2025-09-11 12:45:41', 'create', '2025-09-11 20:45:40', '2025-09-11 20:45:40');
INSERT INTO `user_award_record_000` VALUES (29, 'k18to3_p0z', 100301, 100006, '264946207357', 101, '随机积分', '2025-09-11 12:45:46', 'completed', '2025-09-11 20:45:45', '2025-09-11 20:45:45');
INSERT INTO `user_award_record_000` VALUES (30, 'tbfv9r.lpvfim97', 100301, 100006, '499739330647', 101, '随机积分', '2025-09-11 12:45:46', 'completed', '2025-09-11 20:45:46', '2025-09-11 20:45:46');
INSERT INTO `user_award_record_000` VALUES (31, 'mawnuy_nh7', 100301, 100006, '528496494899', 103, '支付优惠券', '2025-09-11 12:45:49', 'create', '2025-09-11 20:45:49', '2025-09-11 20:45:49');
INSERT INTO `user_award_record_000` VALUES (32, 'i1cvdk.k7f66', 100301, 100006, '399069982632', 101, '随机积分', '2025-09-11 12:45:53', 'completed', '2025-09-11 20:45:52', '2025-09-11 20:45:52');
INSERT INTO `user_award_record_000` VALUES (33, 'lmfmhh.k2j73', 100301, 100006, '831687428725', 103, '支付优惠券', '2025-09-11 12:45:54', 'create', '2025-09-11 20:45:54', '2025-09-11 20:45:54');
INSERT INTO `user_award_record_000` VALUES (34, 'is8k1z_pvp32', 100301, 100006, '663204164119', 101, '随机积分', '2025-09-11 12:45:57', 'completed', '2025-09-11 20:45:56', '2025-09-11 20:45:56');
INSERT INTO `user_award_record_000` VALUES (35, 'lzbmd2_u4x', 100301, 100006, '168815378322', 108, '暴走玩偶', '2025-09-11 12:45:58', 'create', '2025-09-11 20:45:58', '2025-09-11 20:45:58');
INSERT INTO `user_award_record_000` VALUES (36, 'nlrlqi16', 100301, 100006, '609335705023', 108, '暴走玩偶', '2025-09-11 12:46:01', 'create', '2025-09-11 20:46:00', '2025-09-11 20:46:00');
INSERT INTO `user_award_record_000` VALUES (37, 'mawgge_uws', 100301, 100006, '961520270278', 101, '随机积分', '2025-09-11 12:46:02', 'completed', '2025-09-11 20:46:02', '2025-09-11 20:46:02');
INSERT INTO `user_award_record_000` VALUES (38, 'my230', 100301, 100006, '440937137962', 101, '随机积分', '2025-09-11 12:46:10', 'completed', '2025-09-11 20:46:09', '2025-09-11 20:46:09');
INSERT INTO `user_award_record_000` VALUES (39, 'k5qsbt_o3y67', 100301, 100006, '577835539649', 104, '小米台灯', '2025-09-11 12:46:14', 'create', '2025-09-11 20:46:14', '2025-09-11 20:46:14');
INSERT INTO `user_award_record_000` VALUES (40, 'lm1sbt.tcu', 100301, 100006, '955161245416', 103, '支付优惠券', '2025-09-11 12:46:16', 'create', '2025-09-11 20:46:16', '2025-09-11 20:46:16');
INSERT INTO `user_award_record_000` VALUES (41, 'rmftt4.k0m', 100301, 100006, '088093248433', 108, '暴走玩偶', '2025-09-11 12:46:19', 'create', '2025-09-11 20:46:18', '2025-09-11 20:46:18');
INSERT INTO `user_award_record_000` VALUES (42, 'fvpts4_om789', 100301, 100006, '190758392449', 102, 'OpenAI会员卡', '2025-09-11 12:46:25', 'create', '2025-09-11 20:46:25', '2025-09-11 20:46:25');
INSERT INTO `user_award_record_000` VALUES (43, 'ggqlnf_fo574', 100301, 100006, '034820604596', 101, '随机积分', '2025-09-11 12:46:27', 'completed', '2025-09-11 20:46:26', '2025-09-11 20:46:26');
INSERT INTO `user_award_record_000` VALUES (44, 'i1clo517', 100301, 100006, '274326223439', 101, '随机积分', '2025-09-11 12:46:28', 'completed', '2025-09-11 20:46:27', '2025-09-11 20:46:27');
INSERT INTO `user_award_record_000` VALUES (45, 'lm1sds_gii61', 100301, 100006, '888275641024', 103, '支付优惠券', '2025-09-11 12:46:28', 'create', '2025-09-11 20:46:28', '2025-09-11 20:46:28');
INSERT INTO `user_award_record_000` VALUES (46, 'tbfv9r91', 100301, 100006, '755595728593', 103, '支付优惠券', '2025-09-11 12:46:30', 'create', '2025-09-11 20:46:30', '2025-09-11 20:46:30');
INSERT INTO `user_award_record_000` VALUES (47, 'gncsbt51', 100301, 100006, '720279080918', 102, 'OpenAI会员卡', '2025-09-11 12:46:32', 'create', '2025-09-11 20:46:31', '2025-09-11 20:46:31');
INSERT INTO `user_award_record_000` VALUES (48, 'gs2k34.fgw', 100301, 100006, '789544422535', 102, 'OpenAI会员卡', '2025-09-11 12:46:37', 'create', '2025-09-11 20:46:36', '2025-09-11 20:46:36');
INSERT INTO `user_award_record_000` VALUES (49, 'k5qgs270', 100301, 100006, '163734336600', 103, '支付优惠券', '2025-09-11 12:46:40', 'create', '2025-09-11 20:46:39', '2025-09-11 20:46:39');
INSERT INTO `user_award_record_000` VALUES (50, 'irescp.lel27', 100301, 100006, '496349760622', 103, '支付优惠券', '2025-09-11 12:46:48', 'create', '2025-09-11 20:46:48', '2025-09-11 20:46:48');
INSERT INTO `user_award_record_000` VALUES (51, 'fmnrmi35', 100301, 100006, '930968326245', 107, '小霸王游戏机', '2025-09-11 12:46:54', 'create', '2025-09-11 20:46:54', '2025-09-11 20:46:54');
INSERT INTO `user_award_record_000` VALUES (52, 'k8qkfk82', 100301, 100006, '727847101472', 102, 'OpenAI会员卡', '2025-09-11 12:46:59', 'create', '2025-09-11 20:46:59', '2025-09-11 20:46:59');
INSERT INTO `user_award_record_000` VALUES (53, 'irescp_gl4o6y', 100301, 100006, '454556556132', 101, '随机积分', '2025-09-11 12:47:02', 'completed', '2025-09-11 20:47:01', '2025-09-11 20:47:01');
INSERT INTO `user_award_record_000` VALUES (54, 'n0emzg_tog', 100301, 100006, '576059853319', 107, '小霸王游戏机', '2025-09-11 12:47:09', 'create', '2025-09-11 20:47:08', '2025-09-11 20:47:08');
INSERT INTO `user_award_record_000` VALUES (55, 'me4hce.sld', 100301, 100006, '636778115994', 108, '暴走玩偶', '2025-09-11 12:47:12', 'create', '2025-09-11 20:47:12', '2025-09-11 20:47:12');
INSERT INTO `user_award_record_000` VALUES (56, 'fozmy2_o9x53', 100301, 100006, '058878789987', 101, '随机积分', '2025-09-11 12:47:14', 'completed', '2025-09-11 20:47:14', '2025-09-11 20:47:14');
INSERT INTO `user_award_record_000` VALUES (57, 'izhlq416', 100301, 100006, '919634110683', 102, 'OpenAI会员卡', '2025-09-11 12:47:17', 'create', '2025-09-11 20:47:16', '2025-09-11 20:47:16');
INSERT INTO `user_award_record_000` VALUES (58, 'fmnlj154', 100301, 100006, '889895918543', 101, '随机积分', '2025-09-11 12:47:18', 'completed', '2025-09-11 20:47:17', '2025-09-11 20:47:17');
INSERT INTO `user_award_record_000` VALUES (59, 'tbfv9r.ul4', 100301, 100006, '511366015638', 108, '暴走玩偶', '2025-09-11 12:47:18', 'create', '2025-09-11 20:47:18', '2025-09-11 20:47:18');

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
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户中奖记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_award_record_001
-- ----------------------------
INSERT INTO `user_award_record_001` VALUES (1, 'mg0iuc15', 100301, 100006, '751045921382', 101, '随机积分', '2025-09-11 12:43:57', 'completed', '2025-09-11 20:43:56', '2025-09-11 20:43:56');
INSERT INTO `user_award_record_001` VALUES (2, 'o97fmn_q0b37', 100301, 100006, '714351845530', 108, '暴走玩偶', '2025-09-11 12:44:04', 'create', '2025-09-11 20:44:03', '2025-09-11 20:44:03');
INSERT INTO `user_award_record_001` VALUES (3, 'gztmft40', 100301, 100006, '196080466656', 101, '随机积分', '2025-09-11 12:44:05', 'completed', '2025-09-11 20:44:04', '2025-09-11 20:44:04');
INSERT INTO `user_award_record_001` VALUES (4, 'o97kgs32', 100301, 100006, '712724533387', 101, '随机积分', '2025-09-11 12:44:07', 'completed', '2025-09-11 20:44:07', '2025-09-11 20:44:07');
INSERT INTO `user_award_record_001` VALUES (5, 'izhlu897', 100301, 100006, '643627510170', 102, 'OpenAI会员卡', '2025-09-11 12:44:10', 'create', '2025-09-11 20:44:09', '2025-09-11 20:44:09');
INSERT INTO `user_award_record_001` VALUES (6, 'ggqk1z.ijv31', 100301, 100006, '244288706341', 104, '小米台灯', '2025-09-11 12:44:11', 'create', '2025-09-11 20:44:11', '2025-09-11 20:44:11');
INSERT INTO `user_award_record_001` VALUES (7, 'freg6n24', 100301, 100006, '599728785608', 102, 'OpenAI会员卡', '2025-09-11 12:44:12', 'create', '2025-09-11 20:44:12', '2025-09-11 20:44:12');
INSERT INTO `user_award_record_001` VALUES (8, 'feog2w_pet4', 100301, 100006, '049233571966', 107, '小霸王游戏机', '2025-09-11 12:44:23', 'create', '2025-09-11 20:44:23', '2025-09-11 20:44:23');
INSERT INTO `user_award_record_001` VALUES (9, 'tbfnuy.o8678', 100301, 100006, '468320298081', 104, '小米台灯', '2025-09-11 12:44:26', 'create', '2025-09-11 20:44:26', '2025-09-11 20:44:26');
INSERT INTO `user_award_record_001` VALUES (10, 'ui8_noz35', 100301, 100006, '160126513297', 103, '支付优惠券', '2025-09-11 12:44:33', 'create', '2025-09-11 20:44:32', '2025-09-11 20:44:32');
INSERT INTO `user_award_record_001` VALUES (11, 'ts5hvb63', 100301, 100006, '611277238666', 107, '小霸王游戏机', '2025-09-11 12:44:37', 'create', '2025-09-11 20:44:36', '2025-09-11 20:44:36');
INSERT INTO `user_award_record_001` VALUES (12, 'ixjit6_tom', 100301, 100006, '838603739738', 101, '随机积分', '2025-09-11 12:44:40', 'completed', '2025-09-11 20:44:40', '2025-09-11 20:44:40');
INSERT INTO `user_award_record_001` VALUES (13, 'fevfoz_pca37', 100301, 100006, '494353821056', 101, '随机积分', '2025-09-11 12:44:41', 'completed', '2025-09-11 20:44:40', '2025-09-11 20:44:41');
INSERT INTO `user_award_record_001` VALUES (14, 'lm1sbt53', 100301, 100006, '264201005141', 107, '小霸王游戏机', '2025-09-11 12:44:52', 'create', '2025-09-11 20:44:51', '2025-09-11 20:44:51');
INSERT INTO `user_award_record_001` VALUES (15, 'i1cvdk30', 100301, 100006, '556689350107', 101, '随机积分', '2025-09-11 12:44:54', 'completed', '2025-09-11 20:44:54', '2025-09-11 20:44:54');
INSERT INTO `user_award_record_001` VALUES (16, 'irescp_lef33', 100301, 100006, '090265060035', 102, 'OpenAI会员卡', '2025-09-11 12:44:55', 'create', '2025-09-11 20:44:55', '2025-09-11 20:44:55');
INSERT INTO `user_award_record_001` VALUES (17, 'oq3kgn_i6780', 100301, 100006, '491662474781', 101, '随机积分', '2025-09-11 12:44:58', 'completed', '2025-09-11 20:44:58', '2025-09-11 20:44:58');
INSERT INTO `user_award_record_001` VALUES (18, 'i1cuji.kdw21', 100301, 100006, '131812513921', 102, 'OpenAI会员卡', '2025-09-11 12:45:00', 'create', '2025-09-11 20:44:59', '2025-09-11 20:44:59');
INSERT INTO `user_award_record_001` VALUES (19, 'ryiiuk.ml733', 100301, 100006, '720537053052', 101, '随机积分', '2025-09-11 12:45:05', 'completed', '2025-09-11 20:45:05', '2025-09-11 20:45:05');
INSERT INTO `user_award_record_001` VALUES (20, 'irescp.fo890', 100301, 100006, '548845045615', 101, '随机积分', '2025-09-11 12:45:06', 'completed', '2025-09-11 20:45:06', '2025-09-11 20:45:06');
INSERT INTO `user_award_record_001` VALUES (21, 'mzitue_jql', 100301, 100006, '052226621174', 101, '随机积分', '2025-09-11 12:45:07', 'completed', '2025-09-11 20:45:06', '2025-09-11 20:45:07');
INSERT INTO `user_award_record_001` VALUES (22, 'l7nrqi_hrg60', 100301, 100006, '053397780277', 101, '随机积分', '2025-09-11 12:45:08', 'completed', '2025-09-11 20:45:07', '2025-09-11 20:45:07');
INSERT INTO `user_award_record_001` VALUES (23, 'ts5tbf_rvh19', 100301, 100006, '205160331391', 103, '支付优惠券', '2025-09-11 12:45:12', 'create', '2025-09-11 20:45:11', '2025-09-11 20:45:11');
INSERT INTO `user_award_record_001` VALUES (24, 'lm1i2v6', 100301, 100006, '160548526180', 101, '随机积分', '2025-09-11 12:45:12', 'completed', '2025-09-11 20:45:12', '2025-09-11 20:45:12');
INSERT INTO `user_award_record_001` VALUES (25, 'ryiiuk.mlb49', 100301, 100006, '376403963358', 103, '支付优惠券', '2025-09-11 12:45:13', 'create', '2025-09-11 20:45:13', '2025-09-11 20:45:13');
INSERT INTO `user_award_record_001` VALUES (26, 'tgmo3v_jd4', 100301, 100006, '374338139968', 104, '小米台灯', '2025-09-11 12:45:14', 'create', '2025-09-11 20:45:14', '2025-09-11 20:45:14');
INSERT INTO `user_award_record_001` VALUES (27, 'i1cvdk27', 100301, 100006, '969079391077', 108, '暴走玩偶', '2025-09-11 12:45:15', 'create', '2025-09-11 20:45:14', '2025-09-11 20:45:14');
INSERT INTO `user_award_record_001` VALUES (28, 'ggqlnf69', 100301, 100006, '305883833827', 107, '小霸王游戏机', '2025-09-11 12:45:20', 'create', '2025-09-11 20:45:19', '2025-09-11 20:45:19');
INSERT INTO `user_award_record_001` VALUES (29, 'jwui2v10', 100301, 100006, '634231351568', 101, '随机积分', '2025-09-11 12:45:22', 'completed', '2025-09-11 20:45:22', '2025-09-11 20:45:22');
INSERT INTO `user_award_record_001` VALUES (30, 'k1zk5m10', 100301, 100006, '703936929763', 107, '小霸王游戏机', '2025-09-11 12:45:23', 'create', '2025-09-11 20:45:23', '2025-09-11 20:45:23');
INSERT INTO `user_award_record_001` VALUES (31, 'tw9izl.n4v31', 100301, 100006, '972645872059', 101, '随机积分', '2025-09-11 12:45:25', 'completed', '2025-09-11 20:45:24', '2025-09-11 20:45:24');
INSERT INTO `user_award_record_001` VALUES (32, 'pu9ggq.lfs39', 100301, 100006, '860121831868', 101, '随机积分', '2025-09-11 12:45:26', 'completed', '2025-09-11 20:45:25', '2025-09-11 20:45:25');
INSERT INTO `user_award_record_001` VALUES (33, 'i1cvdk63', 100301, 100006, '281965061322', 101, '随机积分', '2025-09-11 12:45:40', 'completed', '2025-09-11 20:45:40', '2025-09-11 20:45:40');
INSERT INTO `user_award_record_001` VALUES (34, 'i1cl5v_fs1', 100301, 100006, '279074109023', 108, '暴走玩偶', '2025-09-11 12:45:42', 'create', '2025-09-11 20:45:41', '2025-09-11 20:45:41');
INSERT INTO `user_award_record_001` VALUES (35, 'hm1i2v_kkh', 100301, 100006, '727261148286', 101, '随机积分', '2025-09-11 12:45:49', 'completed', '2025-09-11 20:45:48', '2025-09-11 20:45:48');
INSERT INTO `user_award_record_001` VALUES (36, 'vacsbt43', 100301, 100006, '089917255530', 101, '随机积分', '2025-09-11 12:45:50', 'completed', '2025-09-11 20:45:50', '2025-09-11 20:45:50');
INSERT INTO `user_award_record_001` VALUES (37, 'k88to374', 100301, 100006, '235274431821', 108, '暴走玩偶', '2025-09-11 12:45:52', 'create', '2025-09-11 20:45:51', '2025-09-11 20:45:51');
INSERT INTO `user_award_record_001` VALUES (38, 'lmfmhh_rwe', 100301, 100006, '292703443524', 103, '支付优惠券', '2025-09-11 12:45:53', 'create', '2025-09-11 20:45:53', '2025-09-11 20:45:53');
INSERT INTO `user_award_record_001` VALUES (39, 'ob5j6i.ik1', 100301, 100006, '882857075127', 107, '小霸王游戏机', '2025-09-11 12:45:55', 'create', '2025-09-11 20:45:55', '2025-09-11 20:45:55');
INSERT INTO `user_award_record_001` VALUES (40, 'o97sbt64', 100301, 100006, '754661956688', 104, '小米台灯', '2025-09-11 12:45:57', 'create', '2025-09-11 20:45:57', '2025-09-11 20:45:57');
INSERT INTO `user_award_record_001` VALUES (41, 'gs2k34.qx879', 100301, 100006, '886237624534', 104, '小米台灯', '2025-09-11 12:45:59', 'create', '2025-09-11 20:45:59', '2025-09-11 20:45:59');
INSERT INTO `user_award_record_001` VALUES (42, 'tgmo3v.l7p33', 100301, 100006, '421460337531', 107, '小霸王游戏机', '2025-09-11 12:46:04', 'create', '2025-09-11 20:46:04', '2025-09-11 20:46:04');
INSERT INTO `user_award_record_001` VALUES (43, 'k7nggq23', 100301, 100006, '077860360760', 104, '小米台灯', '2025-09-11 12:46:05', 'create', '2025-09-11 20:46:04', '2025-09-11 20:46:04');
INSERT INTO `user_award_record_001` VALUES (44, 'mg0iuc_nky', 100301, 100006, '096264337911', 108, '暴走玩偶', '2025-09-11 12:46:09', 'create', '2025-09-11 20:46:08', '2025-09-11 20:46:08');
INSERT INTO `user_award_record_001` VALUES (45, 'p1pscp68', 100301, 100006, '211123330529', 108, '暴走玩偶', '2025-09-11 12:46:11', 'create', '2025-09-11 20:46:11', '2025-09-11 20:46:11');
INSERT INTO `user_award_record_001` VALUES (46, 'lnf59', 100301, 100006, '018577031644', 101, '随机积分', '2025-09-11 12:46:12', 'completed', '2025-09-11 20:46:12', '2025-09-11 20:46:12');
INSERT INTO `user_award_record_001` VALUES (47, 'ffxlmf.puf58', 100301, 100006, '201073428390', 102, 'OpenAI会员卡', '2025-09-11 12:46:13', 'create', '2025-09-11 20:46:13', '2025-09-11 20:46:13');
INSERT INTO `user_award_record_001` VALUES (48, 'sf0ppm_gl296', 100301, 100006, '983424474378', 102, 'OpenAI会员卡', '2025-09-11 12:46:15', 'create', '2025-09-11 20:46:14', '2025-09-11 20:46:14');
INSERT INTO `user_award_record_001` VALUES (49, 'izhsf0.ka036', 100301, 100006, '269441241708', 108, '暴走玩偶', '2025-09-11 12:46:20', 'create', '2025-09-11 20:46:20', '2025-09-11 20:46:20');
INSERT INTO `user_award_record_001` VALUES (50, 'o97kgs_rw844', 100301, 100006, '967736944185', 108, '暴走玩偶', '2025-09-11 12:46:24', 'create', '2025-09-11 20:46:24', '2025-09-11 20:46:24');
INSERT INTO `user_award_record_001` VALUES (51, 'gncsbt25', 100301, 100006, '049427887063', 104, '小米台灯', '2025-09-11 12:46:31', 'create', '2025-09-11 20:46:31', '2025-09-11 20:46:31');
INSERT INTO `user_award_record_001` VALUES (52, 'hm1p8q.mtn', 100301, 100006, '899656495241', 107, '小霸王游戏机', '2025-09-11 12:46:33', 'create', '2025-09-11 20:46:32', '2025-09-11 20:46:32');
INSERT INTO `user_award_record_001` VALUES (53, 'v9rlnf92', 100301, 100006, '373505928385', 103, '支付优惠券', '2025-09-11 12:46:38', 'create', '2025-09-11 20:46:38', '2025-09-11 20:46:38');
INSERT INTO `user_award_record_001` VALUES (54, 'mawgge_itu', 100301, 100006, '502767082134', 103, '支付优惠券', '2025-09-11 12:46:39', 'create', '2025-09-11 20:46:39', '2025-09-11 20:46:39');
INSERT INTO `user_award_record_001` VALUES (55, 'uclgm3_hs4', 100301, 100006, '755330709430', 104, '小米台灯', '2025-09-11 12:46:44', 'create', '2025-09-11 20:46:44', '2025-09-11 20:46:44');
INSERT INTO `user_award_record_001` VALUES (56, 'p1pscp_fo50', 100301, 100006, '804330544240', 101, '随机积分', '2025-09-11 12:46:47', 'completed', '2025-09-11 20:46:47', '2025-09-11 20:46:47');
INSERT INTO `user_award_record_001` VALUES (57, 'jlbffg.nky48', 100301, 100006, '576425970003', 104, '小米台灯', '2025-09-11 12:46:52', 'create', '2025-09-11 20:46:52', '2025-09-11 20:46:52');
INSERT INTO `user_award_record_001` VALUES (58, 'k5qgs271', 100301, 100006, '091079022426', 103, '支付优惠券', '2025-09-11 12:46:57', 'create', '2025-09-11 20:46:56', '2025-09-11 20:46:56');
INSERT INTO `user_award_record_001` VALUES (59, 'k5mmd2.fg017', 100301, 100006, '542194017491', 101, '随机积分', '2025-09-11 12:46:58', 'completed', '2025-09-11 20:46:57', '2025-09-11 20:46:57');
INSERT INTO `user_award_record_001` VALUES (60, 'mg0iuc26', 100301, 100006, '564280232468', 108, '暴走玩偶', '2025-09-11 12:46:58', 'create', '2025-09-11 20:46:58', '2025-09-11 20:46:58');
INSERT INTO `user_award_record_001` VALUES (61, 'l7nrqi26', 100301, 100006, '735090489834', 104, '小米台灯', '2025-09-11 12:47:00', 'create', '2025-09-11 20:47:00', '2025-09-11 20:47:00');
INSERT INTO `user_award_record_001` VALUES (62, 'fevfoz96', 100301, 100006, '192333842642', 108, '暴走玩偶', '2025-09-11 12:47:05', 'create', '2025-09-11 20:47:04', '2025-09-11 20:47:04');
INSERT INTO `user_award_record_001` VALUES (63, 'v9rmdm_fevfrz12', 100301, 100006, '225486083838', 101, '随机积分', '2025-09-11 12:47:07', 'completed', '2025-09-11 20:47:06', '2025-09-11 20:47:06');
INSERT INTO `user_award_record_001` VALUES (64, 'tt6kfy_nyg', 100301, 100006, '974224320960', 101, '随机积分', '2025-09-11 12:47:14', 'completed', '2025-09-11 20:47:13', '2025-09-11 20:47:13');
INSERT INTO `user_award_record_001` VALUES (65, 'fextfv63', 100301, 100006, '323297045715', 102, 'OpenAI会员卡', '2025-09-11 12:47:19', 'create', '2025-09-11 20:47:19', '2025-09-11 20:47:19');
INSERT INTO `user_award_record_001` VALUES (66, 'jlbffg92', 100301, 100006, '517098628988', 101, '随机积分', '2025-09-11 12:47:21', 'completed', '2025-09-11 20:47:21', '2025-09-11 20:47:21');

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
) ENGINE = InnoDB AUTO_INCREMENT = 62 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户中奖记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_award_record_002
-- ----------------------------
INSERT INTO `user_award_record_002` VALUES (1, 'v9blnf.uc9', 100301, 100006, '101549619845', 101, '随机积分', '2025-09-11 12:44:02', 'completed', '2025-09-11 20:44:02', '2025-09-11 20:44:02');
INSERT INTO `user_award_record_002` VALUES (2, 'jlbffg.tu527', 100301, 100006, '591801031170', 101, '随机积分', '2025-09-11 12:44:09', 'completed', '2025-09-11 20:44:08', '2025-09-11 20:44:08');
INSERT INTO `user_award_record_002` VALUES (3, 'ryiiuk88', 100301, 100006, '673171703022', 104, '小米台灯', '2025-09-11 12:44:14', 'create', '2025-09-11 20:44:14', '2025-09-11 20:44:14');
INSERT INTO `user_award_record_002` VALUES (4, 'me4nzu4', 100301, 100006, '168450943720', 108, '暴走玩偶', '2025-09-11 12:44:16', 'create', '2025-09-11 20:44:15', '2025-09-11 20:44:15');
INSERT INTO `user_award_record_002` VALUES (5, 'lmfmhh36', 100301, 100006, '275814493901', 102, 'OpenAI会员卡', '2025-09-11 12:44:20', 'create', '2025-09-11 20:44:20', '2025-09-11 20:44:20');
INSERT INTO `user_award_record_002` VALUES (6, 'm97k5q34', 100301, 100006, '532317029989', 102, 'OpenAI会员卡', '2025-09-11 12:44:21', 'create', '2025-09-11 20:44:21', '2025-09-11 20:44:21');
INSERT INTO `user_award_record_002` VALUES (7, 'fvpki783', 100301, 100006, '226303920407', 107, '小霸王游戏机', '2025-09-11 12:44:22', 'create', '2025-09-11 20:44:22', '2025-09-11 20:44:22');
INSERT INTO `user_award_record_002` VALUES (8, 'tbfnuy_gcf', 100301, 100006, '763544290330', 103, '支付优惠券', '2025-09-11 12:44:25', 'create', '2025-09-11 20:44:24', '2025-09-11 20:44:24');
INSERT INTO `user_award_record_002` VALUES (9, 'ts5hvb.oti', 100301, 100006, '858516683395', 107, '小霸王游戏机', '2025-09-11 12:44:30', 'create', '2025-09-11 20:44:30', '2025-09-11 20:44:30');
INSERT INTO `user_award_record_002` VALUES (10, 'izhsf0.nky82', 100301, 100006, '760840137427', 102, 'OpenAI会员卡', '2025-09-11 12:44:35', 'create', '2025-09-11 20:44:34', '2025-09-11 20:44:34');
INSERT INTO `user_award_record_002` VALUES (11, 'lmfmhh_k28', 100301, 100006, '526427902784', 101, '随机积分', '2025-09-11 12:44:36', 'completed', '2025-09-11 20:44:36', '2025-09-11 20:44:36');
INSERT INTO `user_award_record_002` VALUES (12, 'n0emzg_q0b61', 100301, 100006, '014082357956', 108, '暴走玩偶', '2025-09-11 12:44:39', 'create', '2025-09-11 20:44:39', '2025-09-11 20:44:39');
INSERT INTO `user_award_record_002` VALUES (13, 'v9bmdm_u0617', 100301, 100006, '877777181543', 102, 'OpenAI会员卡', '2025-09-11 12:44:48', 'create', '2025-09-11 20:44:47', '2025-09-11 20:44:47');
INSERT INTO `user_award_record_002` VALUES (14, 'gncsbt31', 100301, 100006, '004062557558', 104, '小米台灯', '2025-09-11 12:44:49', 'create', '2025-09-11 20:44:48', '2025-09-11 20:44:48');
INSERT INTO `user_award_record_002` VALUES (15, 'i1clo591', 100301, 100006, '036385657654', 101, '随机积分', '2025-09-11 12:44:53', 'completed', '2025-09-11 20:44:52', '2025-09-11 20:44:52');
INSERT INTO `user_award_record_002` VALUES (16, 'i1csbt_n5f', 100301, 100006, '608949369459', 101, '随机积分', '2025-09-11 12:44:56', 'completed', '2025-09-11 20:44:55', '2025-09-11 20:44:55');
INSERT INTO `user_award_record_002` VALUES (17, 'lh7lnf87', 100301, 100006, '074459947055', 101, '随机积分', '2025-09-11 12:44:57', 'completed', '2025-09-11 20:44:57', '2025-09-11 20:44:57');
INSERT INTO `user_award_record_002` VALUES (18, 'o97scp.gp8', 100301, 100006, '224063784769', 101, '随机积分', '2025-09-11 12:44:59', 'completed', '2025-09-11 20:44:58', '2025-09-11 20:44:59');
INSERT INTO `user_award_record_002` VALUES (19, 'ob5j6i22', 100301, 100006, '428235789937', 102, 'OpenAI会员卡', '2025-09-11 12:45:17', 'create', '2025-09-11 20:45:16', '2025-09-11 20:45:16');
INSERT INTO `user_award_record_002` VALUES (20, 'ktzmzi69', 100301, 100006, '181300844986', 101, '随机积分', '2025-09-11 12:45:18', 'completed', '2025-09-11 20:45:18', '2025-09-11 20:45:18');
INSERT INTO `user_award_record_002` VALUES (21, 'lm1md2_ggn63', 100301, 100006, '846933349535', 107, '小霸王游戏机', '2025-09-11 12:45:21', 'create', '2025-09-11 20:45:21', '2025-09-11 20:45:21');
INSERT INTO `user_award_record_002` VALUES (22, 'v9rmdm84', 100301, 100006, '062273437164', 108, '暴走玩偶', '2025-09-11 12:45:26', 'create', '2025-09-11 20:45:26', '2025-09-11 20:45:26');
INSERT INTO `user_award_record_002` VALUES (23, 'ttbtw9.i2vk1z', 100301, 100006, '697345318013', 104, '小米台灯', '2025-09-11 12:45:29', 'create', '2025-09-11 20:45:28', '2025-09-11 20:45:28');
INSERT INTO `user_award_record_002` VALUES (24, 'tbfv9r75', 100301, 100006, '519359162157', 102, 'OpenAI会员卡', '2025-09-11 12:45:34', 'create', '2025-09-11 20:45:34', '2025-09-11 20:45:34');
INSERT INTO `user_award_record_002` VALUES (25, 'k7nggq_gc3', 100301, 100006, '636417988062', 108, '暴走玩偶', '2025-09-11 12:45:37', 'create', '2025-09-11 20:45:36', '2025-09-11 20:45:36');
INSERT INTO `user_award_record_002` VALUES (26, 'kgs.fh847', 100301, 100006, '669646618798', 101, '随机积分', '2025-09-11 12:45:42', 'completed', '2025-09-11 20:45:42', '2025-09-11 20:45:42');
INSERT INTO `user_award_record_002` VALUES (27, 'freg6n_k1z', 100301, 100006, '021044035223', 101, '随机积分', '2025-09-11 12:45:43', 'completed', '2025-09-11 20:45:43', '2025-09-11 20:45:43');
INSERT INTO `user_award_record_002` VALUES (28, 'k1zk5m_n54', 100301, 100006, '466909407992', 101, '随机积分', '2025-09-11 12:45:45', 'completed', '2025-09-11 20:45:44', '2025-09-11 20:45:44');
INSERT INTO `user_award_record_002` VALUES (29, 'pu9ggq38', 100301, 100006, '710323640113', 108, '暴走玩偶', '2025-09-11 12:45:47', 'create', '2025-09-11 20:45:47', '2025-09-11 20:45:47');
INSERT INTO `user_award_record_002` VALUES (30, 'o97sbt16', 100301, 100006, '681239720515', 101, '随机积分', '2025-09-11 12:45:51', 'completed', '2025-09-11 20:45:51', '2025-09-11 20:45:51');
INSERT INTO `user_award_record_002` VALUES (31, 'hm1i2v66', 100301, 100006, '566605609703', 101, '随机积分', '2025-09-11 12:45:56', 'completed', '2025-09-11 20:45:55', '2025-09-11 20:45:56');
INSERT INTO `user_award_record_002` VALUES (32, 'p1pscp87', 100301, 100006, '225138803660', 102, 'OpenAI会员卡', '2025-09-11 12:46:00', 'create', '2025-09-11 20:45:59', '2025-09-11 20:45:59');
INSERT INTO `user_award_record_002` VALUES (33, 'i1crqi.i78', 100301, 100006, '943723262091', 107, '小霸王游戏机', '2025-09-11 12:46:03', 'create', '2025-09-11 20:46:03', '2025-09-11 20:46:03');
INSERT INTO `user_award_record_002` VALUES (34, 'hm1nuy_n5c', 100301, 100006, '736193297878', 102, 'OpenAI会员卡', '2025-09-11 12:46:06', 'create', '2025-09-11 20:46:05', '2025-09-11 20:46:05');
INSERT INTO `user_award_record_002` VALUES (35, 'gs2m5617', 100301, 100006, '019337974980', 107, '小霸王游戏机', '2025-09-11 12:46:07', 'create', '2025-09-11 20:46:07', '2025-09-11 20:46:07');
INSERT INTO `user_award_record_002` VALUES (36, 'ggqryd.p8h32', 100301, 100006, '638983123912', 108, '暴走玩偶', '2025-09-11 12:46:16', 'create', '2025-09-11 20:46:15', '2025-09-11 20:46:15');
INSERT INTO `user_award_record_002` VALUES (37, 'izhfoz.r5v', 100301, 100006, '513263877014', 103, '支付优惠券', '2025-09-11 12:46:17', 'create', '2025-09-11 20:46:17', '2025-09-11 20:46:17');
INSERT INTO `user_award_record_002` VALUES (38, 'tw9izl_h9612', 100301, 100006, '452489702941', 108, '暴走玩偶', '2025-09-11 12:46:18', 'create', '2025-09-11 20:46:18', '2025-09-11 20:46:18');
INSERT INTO `user_award_record_002` VALUES (39, 'i1clo5_ny282', 100301, 100006, '628279572336', 103, '支付优惠券', '2025-09-11 12:46:20', 'create', '2025-09-11 20:46:19', '2025-09-11 20:46:19');
INSERT INTO `user_award_record_002` VALUES (40, 'tbfnuy.fof31', 100301, 100006, '145467791412', 101, '随机积分', '2025-09-11 12:46:34', 'completed', '2025-09-11 20:46:34', '2025-09-11 20:46:34');
INSERT INTO `user_award_record_002` VALUES (41, 'ob5j6i.vcr10', 100301, 100006, '703373154788', 102, 'OpenAI会员卡', '2025-09-11 12:46:35', 'create', '2025-09-11 20:46:35', '2025-09-11 20:46:35');
INSERT INTO `user_award_record_002` VALUES (42, 'k1zu0c.fgs', 100301, 100006, '164211006185', 104, '小米台灯', '2025-09-11 12:46:37', 'create', '2025-09-11 20:46:37', '2025-09-11 20:46:37');
INSERT INTO `user_award_record_002` VALUES (43, 'jlbffg15', 100301, 100006, '052819646350', 101, '随机积分', '2025-09-11 12:46:41', 'completed', '2025-09-11 20:46:40', '2025-09-11 20:46:40');
INSERT INTO `user_award_record_002` VALUES (44, 'k4ti7b21', 100301, 100006, '287976874025', 107, '小霸王游戏机', '2025-09-11 12:46:41', 'create', '2025-09-11 20:46:41', '2025-09-11 20:46:41');
INSERT INTO `user_award_record_002` VALUES (45, 'uclgm3.nh4', 100301, 100006, '700913857836', 107, '小霸王游戏机', '2025-09-11 12:46:42', 'create', '2025-09-11 20:46:42', '2025-09-11 20:46:42');
INSERT INTO `user_award_record_002` VALUES (46, 'v9ru7274', 100301, 100006, '224493992388', 101, '随机积分', '2025-09-11 12:46:43', 'completed', '2025-09-11 20:46:43', '2025-09-11 20:46:43');
INSERT INTO `user_award_record_002` VALUES (47, 'v9rmdm.ui4', 100301, 100006, '710456864745', 103, '支付优惠券', '2025-09-11 12:46:45', 'create', '2025-09-11 20:46:44', '2025-09-11 20:46:44');
INSERT INTO `user_award_record_002` VALUES (48, 'rydkjv_kjv91', 100301, 100006, '701788435796', 107, '小霸王游戏机', '2025-09-11 12:46:46', 'create', '2025-09-11 20:46:45', '2025-09-11 20:46:45');
INSERT INTO `user_award_record_002` VALUES (49, 'my250', 100301, 100006, '095855023627', 101, '随机积分', '2025-09-11 12:46:50', 'completed', '2025-09-11 20:46:49', '2025-09-11 20:46:49');
INSERT INTO `user_award_record_002` VALUES (50, 'freui8.kgq', 100301, 100006, '828040489556', 101, '随机积分', '2025-09-11 12:46:50', 'completed', '2025-09-11 20:46:50', '2025-09-11 20:46:50');
INSERT INTO `user_award_record_002` VALUES (51, 'mawfmn_tm050', 100301, 100006, '604453012860', 108, '暴走玩偶', '2025-09-11 12:46:53', 'create', '2025-09-11 20:46:52', '2025-09-11 20:46:52');
INSERT INTO `user_award_record_002` VALUES (52, 'k7znlr_go8', 100301, 100006, '725174156490', 104, '小米台灯', '2025-09-11 12:46:55', 'create', '2025-09-11 20:46:55', '2025-09-11 20:46:55');
INSERT INTO `user_award_record_002` VALUES (53, 'k8qlqi.lld83', 100301, 100006, '683129960506', 104, '小米台灯', '2025-09-11 12:46:56', 'create', '2025-09-11 20:46:56', '2025-09-11 20:46:56');
INSERT INTO `user_award_record_002` VALUES (54, 'ppm_g3a46', 100301, 100006, '909087386821', 102, 'OpenAI会员卡', '2025-09-11 12:47:04', 'create', '2025-09-11 20:47:04', '2025-09-11 20:47:04');
INSERT INTO `user_award_record_002` VALUES (55, 'tw6my2.roi', 100301, 100006, '822355996854', 101, '随机积分', '2025-09-11 12:47:06', 'completed', '2025-09-11 20:47:05', '2025-09-11 20:47:05');
INSERT INTO `user_award_record_002` VALUES (56, 'k7znlr48', 100301, 100006, '909759882751', 101, '随机积分', '2025-09-11 12:47:07', 'completed', '2025-09-11 20:47:07', '2025-09-11 20:47:07');
INSERT INTO `user_award_record_002` VALUES (57, 'ggqryd.twz81', 100301, 100006, '245589492695', 102, 'OpenAI会员卡', '2025-09-11 12:47:08', 'create', '2025-09-11 20:47:08', '2025-09-11 20:47:08');
INSERT INTO `user_award_record_002` VALUES (58, 'p72k3d_qjm', 100301, 100006, '930684869207', 104, '小米台灯', '2025-09-11 12:47:11', 'create', '2025-09-11 20:47:11', '2025-09-11 20:47:11');
INSERT INTO `user_award_record_002` VALUES (59, 'pd6fvp_shb', 100301, 100006, '414109579844', 104, '小米台灯', '2025-09-11 12:47:15', 'create', '2025-09-11 20:47:15', '2025-09-11 20:47:15');
INSERT INTO `user_award_record_002` VALUES (60, 'gs2k345', 100301, 100006, '006237162453', 101, '随机积分', '2025-09-11 12:47:16', 'completed', '2025-09-11 20:47:16', '2025-09-11 20:47:16');
INSERT INTO `user_award_record_002` VALUES (61, 'lm182', 100301, 100006, '329980206213', 101, '随机积分', '2025-09-11 12:47:20', 'completed', '2025-09-11 20:47:20', '2025-09-11 20:47:20');

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
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户中奖记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_award_record_003
-- ----------------------------
INSERT INTO `user_award_record_003` VALUES (1, 'fgpsbt_go848', 100301, 100006, '563332000698', 101, '随机积分', '2025-09-11 12:43:56', 'completed', '2025-09-11 20:43:55', '2025-09-11 20:43:55');
INSERT INTO `user_award_record_003` VALUES (2, 'k1zggq91', 100301, 100006, '903249703262', 104, '小米台灯', '2025-09-11 12:43:57', 'create', '2025-09-11 20:43:57', '2025-09-11 20:43:57');
INSERT INTO `user_award_record_003` VALUES (3, 'm8eiuk.ssq48', 100301, 100006, '076052722368', 101, '随机积分', '2025-09-11 12:43:59', 'completed', '2025-09-11 20:43:59', '2025-09-11 20:43:59');
INSERT INTO `user_award_record_003` VALUES (4, 'pu9ggq31', 100301, 100006, '473878697896', 102, 'OpenAI会员卡', '2025-09-11 12:44:00', 'create', '2025-09-11 20:43:59', '2025-09-11 20:43:59');
INSERT INTO `user_award_record_003` VALUES (5, 'kgs62', 100301, 100006, '833715285056', 101, '随机积分', '2025-09-11 12:44:03', 'completed', '2025-09-11 20:44:03', '2025-09-11 20:44:03');
INSERT INTO `user_award_record_003` VALUES (6, 'tbfnuy.vgw', 100301, 100006, '842809108006', 101, '随机积分', '2025-09-11 12:44:06', 'completed', '2025-09-11 20:44:06', '2025-09-11 20:44:06');
INSERT INTO `user_award_record_003` VALUES (7, 'i2xmy2.r8j', 100301, 100006, '361751462619', 101, '随机积分', '2025-09-11 12:44:11', 'completed', '2025-09-11 20:44:10', '2025-09-11 20:44:10');
INSERT INTO `user_award_record_003` VALUES (8, 'o97sbt73', 100301, 100006, '961670415428', 102, 'OpenAI会员卡', '2025-09-11 12:44:17', 'create', '2025-09-11 20:44:17', '2025-09-11 20:44:17');
INSERT INTO `user_award_record_003` VALUES (9, 'lm1sds.fevfrz', 100301, 100006, '807204192857', 108, '暴走玩偶', '2025-09-11 12:44:19', 'create', '2025-09-11 20:44:19', '2025-09-11 20:44:19');
INSERT INTO `user_award_record_003` VALUES (10, 'pa8kfk.gk458', 100301, 100006, '032030798498', 104, '小米台灯', '2025-09-11 12:44:24', 'create', '2025-09-11 20:44:24', '2025-09-11 20:44:24');
INSERT INTO `user_award_record_003` VALUES (11, 'i1cl5v_oa71', 100301, 100006, '440944749054', 101, '随机积分', '2025-09-11 12:44:27', 'completed', '2025-09-11 20:44:27', '2025-09-11 20:44:27');
INSERT INTO `user_award_record_003` VALUES (12, 'k18iui33', 100301, 100006, '247491259114', 101, '随机积分', '2025-09-11 12:44:28', 'completed', '2025-09-11 20:44:28', '2025-09-11 20:44:28');
INSERT INTO `user_award_record_003` VALUES (13, 'freg6n.pu571', 100301, 100006, '054695370315', 108, '暴走玩偶', '2025-09-11 12:44:29', 'create', '2025-09-11 20:44:28', '2025-09-11 20:44:28');
INSERT INTO `user_award_record_003` VALUES (14, 'k88to3_fj9', 100301, 100006, '275822482545', 102, 'OpenAI会员卡', '2025-09-11 12:44:31', 'create', '2025-09-11 20:44:31', '2025-09-11 20:44:31');
INSERT INTO `user_award_record_003` VALUES (15, 'v9ru72.px5', 100301, 100006, '212127622355', 104, '小米台灯', '2025-09-11 12:44:35', 'create', '2025-09-11 20:44:35', '2025-09-11 20:44:35');
INSERT INTO `user_award_record_003` VALUES (16, 'vacsbt_fse16', 100301, 100006, '009927625959', 104, '小米台灯', '2025-09-11 12:44:38', 'create', '2025-09-11 20:44:37', '2025-09-11 20:44:37');
INSERT INTO `user_award_record_003` VALUES (17, 'jwui2v_g1y56', 100301, 100006, '586799642017', 108, '暴走玩偶', '2025-09-11 12:44:38', 'create', '2025-09-11 20:44:38', '2025-09-11 20:44:38');
INSERT INTO `user_award_record_003` VALUES (18, 'lh7k1z_go8', 100301, 100006, '952614189678', 101, '随机积分', '2025-09-11 12:44:42', 'completed', '2025-09-11 20:44:41', '2025-09-11 20:44:41');
INSERT INTO `user_award_record_003` VALUES (19, 'k8qi48_lid81', 100301, 100006, '936919063532', 103, '支付优惠券', '2025-09-11 12:44:43', 'create', '2025-09-11 20:44:43', '2025-09-11 20:44:43');
INSERT INTO `user_award_record_003` VALUES (20, 'mawgge40', 100301, 100006, '234983687736', 103, '支付优惠券', '2025-09-11 12:44:44', 'create', '2025-09-11 20:44:44', '2025-09-11 20:44:44');
INSERT INTO `user_award_record_003` VALUES (21, 'n0emzg_kfz25', 100301, 100006, '002259037063', 101, '随机积分', '2025-09-11 12:44:45', 'completed', '2025-09-11 20:44:45', '2025-09-11 20:44:45');
INSERT INTO `user_award_record_003` VALUES (22, 'ob5j6i_g7u34', 100301, 100006, '013367367828', 101, '随机积分', '2025-09-11 12:44:46', 'completed', '2025-09-11 20:44:46', '2025-09-11 20:44:46');
INSERT INTO `user_award_record_003` VALUES (23, 'fmni483', 100301, 100006, '361952775166', 101, '随机积分', '2025-09-11 12:44:47', 'completed', '2025-09-11 20:44:46', '2025-09-11 20:44:47');
INSERT INTO `user_award_record_003` VALUES (24, 'k5qscp66', 100301, 100006, '780419895861', 108, '暴走玩偶', '2025-09-11 12:44:51', 'create', '2025-09-11 20:44:51', '2025-09-11 20:44:51');
INSERT INTO `user_award_record_003` VALUES (25, 'ryilj176', 100301, 100006, '880908108216', 104, '小米台灯', '2025-09-11 12:44:53', 'create', '2025-09-11 20:44:53', '2025-09-11 20:44:53');
INSERT INTO `user_award_record_003` VALUES (26, 'freui869', 100301, 100006, '579729086379', 107, '小霸王游戏机', '2025-09-11 12:45:00', 'create', '2025-09-11 20:45:00', '2025-09-11 20:45:00');
INSERT INTO `user_award_record_003` VALUES (27, 'fextfv_fip68', 100301, 100006, '714829104240', 101, '随机积分', '2025-09-11 12:45:03', 'completed', '2025-09-11 20:45:02', '2025-09-11 20:45:02');
INSERT INTO `user_award_record_003` VALUES (28, 'tgmo3v.lpi', 100301, 100006, '015206330380', 104, '小米台灯', '2025-09-11 12:45:08', 'create', '2025-09-11 20:45:08', '2025-09-11 20:45:08');
INSERT INTO `user_award_record_003` VALUES (29, 'p72k3d96', 100301, 100006, '062168416291', 104, '小米台灯', '2025-09-11 12:45:11', 'create', '2025-09-11 20:45:10', '2025-09-11 20:45:10');
INSERT INTO `user_award_record_003` VALUES (30, 'k7fv9r96', 100301, 100006, '192480101955', 108, '暴走玩偶', '2025-09-11 12:45:15', 'create', '2025-09-11 20:45:15', '2025-09-11 20:45:15');
INSERT INTO `user_award_record_003` VALUES (31, 'my2_q11', 100301, 100006, '903330690750', 101, '随机积分', '2025-09-11 12:45:17', 'completed', '2025-09-11 20:45:17', '2025-09-11 20:45:17');
INSERT INTO `user_award_record_003` VALUES (32, 'o97scp.gju20', 100301, 100006, '401414121250', 104, '小米台灯', '2025-09-11 12:45:19', 'create', '2025-09-11 20:45:19', '2025-09-11 20:45:19');
INSERT INTO `user_award_record_003` VALUES (33, 'v9bmdm3', 100301, 100006, '816010943288', 108, '暴走玩偶', '2025-09-11 12:45:21', 'create', '2025-09-11 20:45:20', '2025-09-11 20:45:20');
INSERT INTO `user_award_record_003` VALUES (34, 'k4ti7b6', 100301, 100006, '101706495049', 101, '随机积分', '2025-09-11 12:45:27', 'completed', '2025-09-11 20:45:27', '2025-09-11 20:45:27');
INSERT INTO `user_award_record_003` VALUES (35, 'v9ru72.kdt97', 100301, 100006, '662657061833', 102, 'OpenAI会员卡', '2025-09-11 12:45:30', 'create', '2025-09-11 20:45:29', '2025-09-11 20:45:29');
INSERT INTO `user_award_record_003` VALUES (36, 'gncsbt.lel22', 100301, 100006, '131711098135', 101, '随机积分', '2025-09-11 12:45:30', 'completed', '2025-09-11 20:45:30', '2025-09-11 20:45:30');
INSERT INTO `user_award_record_003` VALUES (37, 'freg6n51', 100301, 100006, '268258696805', 104, '小米台灯', '2025-09-11 12:45:31', 'create', '2025-09-11 20:45:31', '2025-09-11 20:45:31');
INSERT INTO `user_award_record_003` VALUES (38, 'po4sf01', 100301, 100006, '014550644150', 101, '随机积分', '2025-09-11 12:45:32', 'completed', '2025-09-11 20:45:31', '2025-09-11 20:45:32');
INSERT INTO `user_award_record_003` VALUES (39, 'izhlq4.iyo', 100301, 100006, '913494310498', 108, '暴走玩偶', '2025-09-11 12:45:33', 'create', '2025-09-11 20:45:32', '2025-09-11 20:45:32');
INSERT INTO `user_award_record_003` VALUES (40, 'ffxlmf_o1p59', 100301, 100006, '779407547422', 103, '支付优惠券', '2025-09-11 12:45:33', 'create', '2025-09-11 20:45:33', '2025-09-11 20:45:33');
INSERT INTO `user_award_record_003` VALUES (41, 'nlrlqi31', 100301, 100006, '256041089293', 101, '随机积分', '2025-09-11 12:45:35', 'completed', '2025-09-11 20:45:35', '2025-09-11 20:45:35');
INSERT INTO `user_award_record_003` VALUES (42, 'ucop8d83', 100301, 100006, '232504766036', 103, '支付优惠券', '2025-09-11 12:45:38', 'create', '2025-09-11 20:45:37', '2025-09-11 20:45:37');
INSERT INTO `user_award_record_003` VALUES (43, 'i2xmy2.u6m', 100301, 100006, '000823273837', 104, '小米台灯', '2025-09-11 12:45:39', 'create', '2025-09-11 20:45:39', '2025-09-11 20:45:39');
INSERT INTO `user_award_record_003` VALUES (44, 'p72k3d.uc9', 100301, 100006, '332375558446', 101, '随机积分', '2025-09-11 12:45:44', 'completed', '2025-09-11 20:45:44', '2025-09-11 20:45:44');
INSERT INTO `user_award_record_003` VALUES (45, 'k8qlqi2', 100301, 100006, '968533123052', 107, '小霸王游戏机', '2025-09-11 12:45:48', 'create', '2025-09-11 20:45:48', '2025-09-11 20:45:48');
INSERT INTO `user_award_record_003` VALUES (46, 'k5mit6.fke', 100301, 100006, '521314442205', 103, '支付优惠券', '2025-09-11 12:46:02', 'create', '2025-09-11 20:46:01', '2025-09-11 20:46:01');
INSERT INTO `user_award_record_003` VALUES (47, 'gs2m565', 100301, 100006, '096775402100', 101, '随机积分', '2025-09-11 12:46:06', 'completed', '2025-09-11 20:46:06', '2025-09-11 20:46:06');
INSERT INTO `user_award_record_003` VALUES (48, 'jcvsbt.glt95', 100301, 100006, '727773414522', 107, '小霸王游戏机', '2025-09-11 12:46:08', 'create', '2025-09-11 20:46:08', '2025-09-11 20:46:08');
INSERT INTO `user_award_record_003` VALUES (49, 'i1cvdk3', 100301, 100006, '906392245153', 103, '支付优惠券', '2025-09-11 12:46:10', 'create', '2025-09-11 20:46:10', '2025-09-11 20:46:10');
INSERT INTO `user_award_record_003` VALUES (50, 'feog2w_tfa', 100301, 100006, '162334120495', 101, '随机积分', '2025-09-11 12:46:21', 'completed', '2025-09-11 20:46:21', '2025-09-11 20:46:21');
INSERT INTO `user_award_record_003` VALUES (51, 'fvpts463', 100301, 100006, '602546447780', 102, 'OpenAI会员卡', '2025-09-11 12:46:22', 'create', '2025-09-11 20:46:22', '2025-09-11 20:46:22');
INSERT INTO `user_award_record_003` VALUES (52, 'ktzmzi_h96', 100301, 100006, '568965923397', 101, '随机积分', '2025-09-11 12:46:23', 'completed', '2025-09-11 20:46:22', '2025-09-11 20:46:22');
INSERT INTO `user_award_record_003` VALUES (53, 'kgs.ojx', 100301, 100006, '807637026925', 101, '随机积分', '2025-09-11 12:46:24', 'completed', '2025-09-11 20:46:23', '2025-09-11 20:46:23');
INSERT INTO `user_award_record_003` VALUES (54, 'tt6kfy.oq372', 100301, 100006, '244152113303', 102, 'OpenAI会员卡', '2025-09-11 12:46:26', 'create', '2025-09-11 20:46:26', '2025-09-11 20:46:26');
INSERT INTO `user_award_record_003` VALUES (55, 'pu9ggq_s3t71', 100301, 100006, '788059486915', 102, 'OpenAI会员卡', '2025-09-11 12:46:29', 'create', '2025-09-11 20:46:29', '2025-09-11 20:46:29');
INSERT INTO `user_award_record_003` VALUES (56, 'tgmo3v.ptd0', 100301, 100006, '874913292666', 101, '随机积分', '2025-09-11 12:46:33', 'completed', '2025-09-11 20:46:33', '2025-09-11 20:46:33');
INSERT INTO `user_award_record_003` VALUES (57, 'jlbi46.g30i1l', 100301, 100006, '040363070521', 104, '小米台灯', '2025-09-11 12:46:36', 'create', '2025-09-11 20:46:35', '2025-09-11 20:46:35');
INSERT INTO `user_award_record_003` VALUES (58, 'k7znlr38', 100301, 100006, '761270124687', 103, '支付优惠券', '2025-09-11 12:46:46', 'create', '2025-09-11 20:46:46', '2025-09-11 20:46:46');
INSERT INTO `user_award_record_003` VALUES (59, 'ts5tbf43', 100301, 100006, '848741623647', 104, '小米台灯', '2025-09-11 12:46:49', 'create', '2025-09-11 20:46:48', '2025-09-11 20:46:48');
INSERT INTO `user_award_record_003` VALUES (60, 'lm1_sny', 100301, 100006, '227357598813', 108, '暴走玩偶', '2025-09-11 12:46:51', 'create', '2025-09-11 20:46:51', '2025-09-11 20:46:51');
INSERT INTO `user_award_record_003` VALUES (61, 'ttbtw9_k6731', 100301, 100006, '508237237670', 104, '小米台灯', '2025-09-11 12:47:01', 'create', '2025-09-11 20:47:00', '2025-09-11 20:47:00');
INSERT INTO `user_award_record_003` VALUES (62, 'ob5j6i_nevn5742', 100301, 100006, '223186507909', 103, '支付优惠券', '2025-09-11 12:47:02', 'create', '2025-09-11 20:47:02', '2025-09-11 20:47:02');
INSERT INTO `user_award_record_003` VALUES (63, 'fmni4885', 100301, 100006, '014212327196', 101, '随机积分', '2025-09-11 12:47:03', 'completed', '2025-09-11 20:47:03', '2025-09-11 20:47:03');
INSERT INTO `user_award_record_003` VALUES (64, 'o97rmi.gbf98', 100301, 100006, '935817324091', 104, '小米台灯', '2025-09-11 12:47:10', 'create', '2025-09-11 20:47:09', '2025-09-11 20:47:09');
INSERT INTO `user_award_record_003` VALUES (65, 'pd6fvp_gky51', 100301, 100006, '029694653747', 104, '小米台灯', '2025-09-11 12:47:10', 'create', '2025-09-11 20:47:10', '2025-09-11 20:47:10');
INSERT INTO `user_award_record_003` VALUES (66, 'fmnrmi_vcr', 100301, 100006, '510999145303', 104, '小米台灯', '2025-09-11 12:47:13', 'create', '2025-09-11 20:47:12', '2025-09-11 20:47:12');

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
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户行为返利流水订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_behavior_rebate_order_000
-- ----------------------------
INSERT INTO `user_behavior_rebate_order_000` VALUES (5, 'xiaofuge1', '883971522401', 'sign', '签到返利-sku额度', 'sku', '9011', '20240503', 'xiaofuge1_sku_20240503', '2024-05-03 16:01:42', '2024-05-03 16:01:42');
INSERT INTO `user_behavior_rebate_order_000` VALUES (6, 'xiaofuge1', '995944930386', 'sign', '签到返利-积分', 'integral', '10', '20240503', 'xiaofuge1_integral_20240503', '2024-05-03 16:01:43', '2024-05-03 16:01:43');
INSERT INTO `user_behavior_rebate_order_000` VALUES (7, 'user001', '153309768984', 'sign', '签到返利-sku额度', 'sku', '9011', '20240524', 'user001_sku_20240524', '2024-05-24 22:25:09', '2024-05-24 22:25:09');
INSERT INTO `user_behavior_rebate_order_000` VALUES (8, 'user001', '922397055482', 'sign', '签到返利-积分', 'integral', '10', '20240524', 'user001_integral_20240524', '2024-05-24 22:25:09', '2024-05-24 22:25:09');

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
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户行为返利流水订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_behavior_rebate_order_001
-- ----------------------------
INSERT INTO `user_behavior_rebate_order_001` VALUES (1, 'user002', '791847185524', 'sign', '签到返利-sku额度', 'sku', '9011', '20240524', 'user002_sku_20240524', '2024-05-24 22:29:11', '2024-05-24 22:29:11');
INSERT INTO `user_behavior_rebate_order_001` VALUES (2, 'user002', '119018837248', 'sign', '签到返利-积分', 'integral', '10', '20240524', 'user002_integral_20240524', '2024-05-24 22:29:11', '2024-05-24 22:29:11');
INSERT INTO `user_behavior_rebate_order_001` VALUES (3, 'user002', '031618502370', 'sign', '签到返利-sku额度', 'sku', '9011', '20240527', 'user002_sku_20240527', '2024-05-27 22:37:06', '2024-05-27 22:37:06');
INSERT INTO `user_behavior_rebate_order_001` VALUES (4, 'user002', '591157608900', 'sign', '签到返利-积分', 'integral', '10', '20240527', 'user002_integral_20240527', '2024-05-27 22:37:07', '2024-05-27 22:37:07');

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户行为返利流水订单表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户行为返利流水订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_behavior_rebate_order_003
-- ----------------------------
INSERT INTO `user_behavior_rebate_order_003` VALUES (7, 'xiaofuge2', '298513989210', 'sign', '签到返利-sku额度', 'sku', '9011', '20240504', 'xiaofuge2_sku_20240504', '2024-05-04 13:07:53', '2024-05-04 13:07:53');
INSERT INTO `user_behavior_rebate_order_003` VALUES (8, 'xiaofuge2', '352651244433', 'sign', '签到返利-积分', 'integral', '10', '20240504', 'xiaofuge2_integral_20240504', '2024-05-04 13:07:53', '2024-05-04 13:07:53');

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
) ENGINE = InnoDB AUTO_INCREMENT = 94 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户积分账户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_credit_account
-- ----------------------------
INSERT INTO `user_credit_account` VALUES (2, 'user001', 3.65, 3.65, 'open', '2024-05-24 22:34:19', '2024-05-30 07:22:10');
INSERT INTO `user_credit_account` VALUES (3, 'fgpsbt_go848', 20.00, 20.00, 'open', '2025-09-11 20:43:55', '2025-09-11 20:43:55');
INSERT INTO `user_credit_account` VALUES (4, 'mg0iuc15', 8.86, 8.86, 'open', '2025-09-11 20:43:56', '2025-09-11 20:43:56');
INSERT INTO `user_credit_account` VALUES (5, 'ppm52', 28.40, 28.40, 'open', '2025-09-11 20:43:58', '2025-09-11 20:43:58');
INSERT INTO `user_credit_account` VALUES (6, 'm8eiuk.ssq48', 58.20, 58.20, 'open', '2025-09-11 20:43:59', '2025-09-11 20:43:59');
INSERT INTO `user_credit_account` VALUES (7, 'izhlq4_gii', 41.20, 41.20, 'open', '2025-09-11 20:44:00', '2025-09-11 20:44:00');
INSERT INTO `user_credit_account` VALUES (8, 'v9blnf.uc9', 55.00, 55.00, 'open', '2025-09-11 20:44:02', '2025-09-11 20:44:02');
INSERT INTO `user_credit_account` VALUES (9, 'kgs62', 14.80, 14.80, 'open', '2025-09-11 20:44:03', '2025-09-11 20:44:03');
INSERT INTO `user_credit_account` VALUES (10, 'gztmft40', 43.00, 43.00, 'open', '2025-09-11 20:44:04', '2025-09-11 20:44:04');
INSERT INTO `user_credit_account` VALUES (11, 'tbfnuy.vgw', 32.00, 32.00, 'open', '2025-09-11 20:44:06', '2025-09-11 20:44:06');
INSERT INTO `user_credit_account` VALUES (12, 'o97kgs32', 70.20, 70.20, 'open', '2025-09-11 20:44:07', '2025-09-11 20:44:07');
INSERT INTO `user_credit_account` VALUES (13, 'jlbffg.tu527', 39.40, 39.40, 'open', '2025-09-11 20:44:08', '2025-09-11 20:44:08');
INSERT INTO `user_credit_account` VALUES (14, 'i2xmy2.r8j', 70.70, 70.70, 'open', '2025-09-11 20:44:10', '2025-09-11 20:44:10');
INSERT INTO `user_credit_account` VALUES (15, 'lm1sbt_seo', 16.00, 16.00, 'open', '2025-09-11 20:44:15', '2025-09-11 20:44:15');
INSERT INTO `user_credit_account` VALUES (16, 'i1cl5v_oa71', 67.50, 67.50, 'open', '2025-09-11 20:44:27', '2025-09-11 20:44:27');
INSERT INTO `user_credit_account` VALUES (17, 'k18iui33', 69.30, 69.30, 'open', '2025-09-11 20:44:28', '2025-09-11 20:44:28');
INSERT INTO `user_credit_account` VALUES (18, 'ucop8d_skb26', 30.20, 30.20, 'open', '2025-09-11 20:44:29', '2025-09-11 20:44:29');
INSERT INTO `user_credit_account` VALUES (19, 'jzzgm3_kls', 74.80, 74.80, 'open', '2025-09-11 20:44:32', '2025-09-11 20:44:32');
INSERT INTO `user_credit_account` VALUES (20, 'hm1p8q25', 88.00, 88.00, 'open', '2025-09-11 20:44:33', '2025-09-11 20:44:33');
INSERT INTO `user_credit_account` VALUES (21, 'lmfmhh_k28', 62.10, 62.10, 'open', '2025-09-11 20:44:36', '2025-09-11 20:44:36');
INSERT INTO `user_credit_account` VALUES (22, 'ixjit6_tom', 43.60, 43.60, 'open', '2025-09-11 20:44:40', '2025-09-11 20:44:40');
INSERT INTO `user_credit_account` VALUES (23, 'fevfoz_pca37', 63.60, 63.60, 'open', '2025-09-11 20:44:41', '2025-09-11 20:44:41');
INSERT INTO `user_credit_account` VALUES (24, 'lh7k1z_go8', 83.70, 83.70, 'open', '2025-09-11 20:44:41', '2025-09-11 20:44:41');
INSERT INTO `user_credit_account` VALUES (25, 'n0emzg_kfz25', 84.10, 84.10, 'open', '2025-09-11 20:44:45', '2025-09-11 20:44:45');
INSERT INTO `user_credit_account` VALUES (26, 'ob5j6i_g7u34', 26.30, 26.30, 'open', '2025-09-11 20:44:46', '2025-09-11 20:44:46');
INSERT INTO `user_credit_account` VALUES (27, 'fmni483', 51.00, 51.00, 'open', '2025-09-11 20:44:47', '2025-09-11 20:44:47');
INSERT INTO `user_credit_account` VALUES (28, 'lh7lnf81', 73.00, 73.00, 'open', '2025-09-11 20:44:49', '2025-09-11 20:44:49');
INSERT INTO `user_credit_account` VALUES (29, 'i1clo591', 21.00, 21.00, 'open', '2025-09-11 20:44:52', '2025-09-11 20:44:52');
INSERT INTO `user_credit_account` VALUES (30, 'i1cvdk30', 74.90, 74.90, 'open', '2025-09-11 20:44:54', '2025-09-11 20:44:54');
INSERT INTO `user_credit_account` VALUES (31, 'i1csbt_n5f', 79.70, 79.70, 'open', '2025-09-11 20:44:55', '2025-09-11 20:44:55');
INSERT INTO `user_credit_account` VALUES (32, 'ggqryd.rwe1', 78.60, 78.60, 'open', '2025-09-11 20:44:56', '2025-09-11 20:44:56');
INSERT INTO `user_credit_account` VALUES (33, 'lh7lnf87', 50.60, 50.60, 'open', '2025-09-11 20:44:57', '2025-09-11 20:44:57');
INSERT INTO `user_credit_account` VALUES (34, 'oq3kgn_i6780', 47.40, 47.40, 'open', '2025-09-11 20:44:58', '2025-09-11 20:44:58');
INSERT INTO `user_credit_account` VALUES (35, 'o97scp.gp8', 97.80, 97.80, 'open', '2025-09-11 20:44:59', '2025-09-11 20:44:59');
INSERT INTO `user_credit_account` VALUES (36, 'k7znlr46', 99.80, 99.80, 'open', '2025-09-11 20:45:02', '2025-09-11 20:45:02');
INSERT INTO `user_credit_account` VALUES (37, 'fextfv_fip68', 17.70, 17.70, 'open', '2025-09-11 20:45:02', '2025-09-11 20:45:02');
INSERT INTO `user_credit_account` VALUES (38, 'p1pvgw.fkp22', 52.40, 52.40, 'open', '2025-09-11 20:45:03', '2025-09-11 20:45:03');
INSERT INTO `user_credit_account` VALUES (39, 'ryiiuk.ml733', 43.50, 43.50, 'open', '2025-09-11 20:45:05', '2025-09-11 20:45:05');
INSERT INTO `user_credit_account` VALUES (40, 'irescp.fo890', 10.50, 10.50, 'open', '2025-09-11 20:45:06', '2025-09-11 20:45:06');
INSERT INTO `user_credit_account` VALUES (41, 'mzitue_jql', 49.00, 49.00, 'open', '2025-09-11 20:45:07', '2025-09-11 20:45:07');
INSERT INTO `user_credit_account` VALUES (42, 'l7nrqi_hrg60', 3.69, 3.69, 'open', '2025-09-11 20:45:07', '2025-09-11 20:45:07');
INSERT INTO `user_credit_account` VALUES (43, 'lm1i2v6', 51.50, 51.50, 'open', '2025-09-11 20:45:12', '2025-09-11 20:45:12');
INSERT INTO `user_credit_account` VALUES (44, 'my2_q11', 71.10, 71.10, 'open', '2025-09-11 20:45:17', '2025-09-11 20:45:17');
INSERT INTO `user_credit_account` VALUES (45, 'ktzmzi69', 38.70, 38.70, 'open', '2025-09-11 20:45:18', '2025-09-11 20:45:18');
INSERT INTO `user_credit_account` VALUES (46, 'jwui2v10', 97.70, 97.70, 'open', '2025-09-11 20:45:22', '2025-09-11 20:45:22');
INSERT INTO `user_credit_account` VALUES (47, 'tw9izl.n4v31', 83.70, 83.70, 'open', '2025-09-11 20:45:24', '2025-09-11 20:45:24');
INSERT INTO `user_credit_account` VALUES (48, 'pu9ggq.lfs39', 62.00, 62.00, 'open', '2025-09-11 20:45:25', '2025-09-11 20:45:25');
INSERT INTO `user_credit_account` VALUES (49, 'k4ti7b6', 38.70, 38.70, 'open', '2025-09-11 20:45:27', '2025-09-11 20:45:27');
INSERT INTO `user_credit_account` VALUES (50, 'gncsbt.lel22', 73.50, 73.50, 'open', '2025-09-11 20:45:30', '2025-09-11 20:45:30');
INSERT INTO `user_credit_account` VALUES (51, 'po4sf01', 61.70, 61.70, 'open', '2025-09-11 20:45:32', '2025-09-11 20:45:32');
INSERT INTO `user_credit_account` VALUES (52, 'nlrlqi31', 57.40, 57.40, 'open', '2025-09-11 20:45:35', '2025-09-11 20:45:35');
INSERT INTO `user_credit_account` VALUES (53, 'i1cvdk63', 73.10, 73.10, 'open', '2025-09-11 20:45:40', '2025-09-11 20:45:40');
INSERT INTO `user_credit_account` VALUES (54, 'kgs.fh847', 60.80, 60.80, 'open', '2025-09-11 20:45:42', '2025-09-11 20:45:42');
INSERT INTO `user_credit_account` VALUES (55, 'freg6n_k1z', 29.80, 29.80, 'open', '2025-09-11 20:45:43', '2025-09-11 20:45:43');
INSERT INTO `user_credit_account` VALUES (56, 'p72k3d.uc9', 62.10, 62.10, 'open', '2025-09-11 20:45:44', '2025-09-11 20:45:44');
INSERT INTO `user_credit_account` VALUES (57, 'k1zk5m_n54', 30.30, 30.30, 'open', '2025-09-11 20:45:44', '2025-09-11 20:45:44');
INSERT INTO `user_credit_account` VALUES (58, 'k18to3_p0z', 95.00, 95.00, 'open', '2025-09-11 20:45:45', '2025-09-11 20:45:45');
INSERT INTO `user_credit_account` VALUES (59, 'tbfv9r.lpvfim97', 70.70, 70.70, 'open', '2025-09-11 20:45:46', '2025-09-11 20:45:46');
INSERT INTO `user_credit_account` VALUES (60, 'hm1i2v_kkh', 13.30, 13.30, 'open', '2025-09-11 20:45:48', '2025-09-11 20:45:48');
INSERT INTO `user_credit_account` VALUES (61, 'vacsbt43', 44.50, 44.50, 'open', '2025-09-11 20:45:50', '2025-09-11 20:45:50');
INSERT INTO `user_credit_account` VALUES (62, 'o97sbt16', 84.20, 84.20, 'open', '2025-09-11 20:45:51', '2025-09-11 20:45:51');
INSERT INTO `user_credit_account` VALUES (63, 'i1cvdk.k7f66', 38.00, 38.00, 'open', '2025-09-11 20:45:52', '2025-09-11 20:45:52');
INSERT INTO `user_credit_account` VALUES (64, 'hm1i2v66', 65.70, 65.70, 'open', '2025-09-11 20:45:56', '2025-09-11 20:45:56');
INSERT INTO `user_credit_account` VALUES (65, 'is8k1z_pvp32', 56.70, 56.70, 'open', '2025-09-11 20:45:56', '2025-09-11 20:45:56');
INSERT INTO `user_credit_account` VALUES (66, 'mawgge_uws', 6.81, 6.81, 'open', '2025-09-11 20:46:02', '2025-09-11 20:46:02');
INSERT INTO `user_credit_account` VALUES (67, 'gs2m565', 59.60, 59.60, 'open', '2025-09-11 20:46:06', '2025-09-11 20:46:06');
INSERT INTO `user_credit_account` VALUES (68, 'my230', 63.60, 63.60, 'open', '2025-09-11 20:46:09', '2025-09-11 20:46:09');
INSERT INTO `user_credit_account` VALUES (69, 'lnf59', 79.00, 79.00, 'open', '2025-09-11 20:46:12', '2025-09-11 20:46:12');
INSERT INTO `user_credit_account` VALUES (70, 'feog2w_tfa', 24.60, 24.60, 'open', '2025-09-11 20:46:21', '2025-09-11 20:46:21');
INSERT INTO `user_credit_account` VALUES (71, 'ktzmzi_h96', 47.10, 47.10, 'open', '2025-09-11 20:46:22', '2025-09-11 20:46:22');
INSERT INTO `user_credit_account` VALUES (72, 'kgs.ojx', 35.40, 35.40, 'open', '2025-09-11 20:46:23', '2025-09-11 20:46:23');
INSERT INTO `user_credit_account` VALUES (73, 'ggqlnf_fo574', 81.20, 81.20, 'open', '2025-09-11 20:46:26', '2025-09-11 20:46:26');
INSERT INTO `user_credit_account` VALUES (74, 'i1clo517', 31.60, 31.60, 'open', '2025-09-11 20:46:27', '2025-09-11 20:46:27');
INSERT INTO `user_credit_account` VALUES (75, 'tgmo3v.ptd0', 14.60, 14.60, 'open', '2025-09-11 20:46:33', '2025-09-11 20:46:33');
INSERT INTO `user_credit_account` VALUES (76, 'tbfnuy.fof31', 11.50, 11.50, 'open', '2025-09-11 20:46:34', '2025-09-11 20:46:34');
INSERT INTO `user_credit_account` VALUES (77, 'jlbffg15', 68.30, 68.30, 'open', '2025-09-11 20:46:40', '2025-09-11 20:46:40');
INSERT INTO `user_credit_account` VALUES (78, 'v9ru7274', 1.73, 1.73, 'open', '2025-09-11 20:46:43', '2025-09-11 20:46:43');
INSERT INTO `user_credit_account` VALUES (79, 'p1pscp_fo50', 99.20, 99.20, 'open', '2025-09-11 20:46:47', '2025-09-11 20:46:47');
INSERT INTO `user_credit_account` VALUES (80, 'my250', 56.30, 56.30, 'open', '2025-09-11 20:46:49', '2025-09-11 20:46:49');
INSERT INTO `user_credit_account` VALUES (81, 'freui8.kgq', 2.82, 2.82, 'open', '2025-09-11 20:46:50', '2025-09-11 20:46:50');
INSERT INTO `user_credit_account` VALUES (82, 'k5mmd2.fg017', 9.79, 9.79, 'open', '2025-09-11 20:46:57', '2025-09-11 20:46:57');
INSERT INTO `user_credit_account` VALUES (83, 'irescp_gl4o6y', 78.40, 78.40, 'open', '2025-09-11 20:47:01', '2025-09-11 20:47:01');
INSERT INTO `user_credit_account` VALUES (84, 'fmni4885', 7.08, 7.08, 'open', '2025-09-11 20:47:03', '2025-09-11 20:47:03');
INSERT INTO `user_credit_account` VALUES (85, 'tw6my2.roi', 89.80, 89.80, 'open', '2025-09-11 20:47:05', '2025-09-11 20:47:05');
INSERT INTO `user_credit_account` VALUES (86, 'v9rmdm_fevfrz12', 10.00, 10.00, 'open', '2025-09-11 20:47:06', '2025-09-11 20:47:06');
INSERT INTO `user_credit_account` VALUES (87, 'k7znlr48', 23.70, 23.70, 'open', '2025-09-11 20:47:07', '2025-09-11 20:47:07');
INSERT INTO `user_credit_account` VALUES (88, 'tt6kfy_nyg', 67.80, 67.80, 'open', '2025-09-11 20:47:13', '2025-09-11 20:47:13');
INSERT INTO `user_credit_account` VALUES (89, 'fozmy2_o9x53', 44.40, 44.40, 'open', '2025-09-11 20:47:14', '2025-09-11 20:47:14');
INSERT INTO `user_credit_account` VALUES (90, 'gs2k345', 90.80, 90.80, 'open', '2025-09-11 20:47:16', '2025-09-11 20:47:16');
INSERT INTO `user_credit_account` VALUES (91, 'fmnlj154', 89.30, 89.30, 'open', '2025-09-11 20:47:17', '2025-09-11 20:47:17');
INSERT INTO `user_credit_account` VALUES (92, 'lm182', 83.60, 83.60, 'open', '2025-09-11 20:47:20', '2025-09-11 20:47:20');
INSERT INTO `user_credit_account` VALUES (93, 'jlbffg92', 97.60, 97.60, 'open', '2025-09-11 20:47:21', '2025-09-11 20:47:21');

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户积分订单记录' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户积分订单记录' ROW_FORMAT = Dynamic;

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
INSERT INTO `user_raffle_order_000` VALUES (1, 'ppm52', 100301, '测试活动', 100006, '089122622827', '2025-09-11 12:43:58', 'used', '2025-09-11 20:43:57', '2025-09-11 20:43:58');
INSERT INTO `user_raffle_order_000` VALUES (2, 'izhlq4_gii', 100301, '测试活动', 100006, '937148513755', '2025-09-11 12:44:00', 'used', '2025-09-11 20:44:00', '2025-09-11 20:44:00');
INSERT INTO `user_raffle_order_000` VALUES (3, 'jzzgm357', 100301, '测试活动', 100006, '285290209479', '2025-09-11 12:44:01', 'used', '2025-09-11 20:44:01', '2025-09-11 20:44:01');
INSERT INTO `user_raffle_order_000` VALUES (4, 'izhlq4.rxn', 100301, '测试活动', 100006, '392717571649', '2025-09-11 12:44:05', 'used', '2025-09-11 20:44:05', '2025-09-11 20:44:05');
INSERT INTO `user_raffle_order_000` VALUES (5, 'ggqlnf_tca', 100301, '测试活动', 100006, '363170555865', '2025-09-11 12:44:08', 'used', '2025-09-11 20:44:07', '2025-09-11 20:44:08');
INSERT INTO `user_raffle_order_000` VALUES (6, 'k8qi488', 100301, '测试活动', 100006, '989817973678', '2025-09-11 12:44:13', 'used', '2025-09-11 20:44:12', '2025-09-11 20:44:13');
INSERT INTO `user_raffle_order_000` VALUES (7, 'lm1sbt_seo', 100301, '测试活动', 100006, '060715855252', '2025-09-11 12:44:14', 'used', '2025-09-11 20:44:14', '2025-09-11 20:44:14');
INSERT INTO `user_raffle_order_000` VALUES (8, 'k8qscp8', 100301, '测试活动', 100006, '353860910031', '2025-09-11 12:44:16', 'used', '2025-09-11 20:44:16', '2025-09-11 20:44:16');
INSERT INTO `user_raffle_order_000` VALUES (9, 'izh38', 100301, '测试活动', 100006, '130835750784', '2025-09-11 12:44:18', 'used', '2025-09-11 20:44:18', '2025-09-11 20:44:18');
INSERT INTO `user_raffle_order_000` VALUES (10, 'k1zggq8', 100301, '测试活动', 100006, '139380037709', '2025-09-11 12:44:25', 'used', '2025-09-11 20:44:25', '2025-09-11 20:44:25');
INSERT INTO `user_raffle_order_000` VALUES (11, 'ucop8d_skb26', 100301, '测试活动', 100006, '847493615368', '2025-09-11 12:44:29', 'used', '2025-09-11 20:44:29', '2025-09-11 20:44:29');
INSERT INTO `user_raffle_order_000` VALUES (12, 'jzzgm3_kls', 100301, '测试活动', 100006, '601722234977', '2025-09-11 12:44:32', 'used', '2025-09-11 20:44:31', '2025-09-11 20:44:32');
INSERT INTO `user_raffle_order_000` VALUES (13, 'hm1p8q25', 100301, '测试活动', 100006, '695015150623', '2025-09-11 12:44:33', 'used', '2025-09-11 20:44:33', '2025-09-11 20:44:33');
INSERT INTO `user_raffle_order_000` VALUES (14, 'mawtue.tfa', 100301, '测试活动', 100006, '761928899734', '2025-09-11 12:44:42', 'used', '2025-09-11 20:44:42', '2025-09-11 20:44:42');
INSERT INTO `user_raffle_order_000` VALUES (15, 'lh7lnf81', 100301, '测试活动', 100006, '047229855366', '2025-09-11 12:44:49', 'used', '2025-09-11 20:44:49', '2025-09-11 20:44:49');
INSERT INTO `user_raffle_order_000` VALUES (16, 'ixjit6.ldu80', 100301, '测试活动', 100006, '278057521875', '2025-09-11 12:44:50', 'used', '2025-09-11 20:44:49', '2025-09-11 20:44:50');
INSERT INTO `user_raffle_order_000` VALUES (17, 'ggqryd.rwe1', 100301, '测试活动', 100006, '080027856118', '2025-09-11 12:44:56', 'used', '2025-09-11 20:44:56', '2025-09-11 20:44:56');
INSERT INTO `user_raffle_order_000` VALUES (18, 'kgs.i60', 100301, '测试活动', 100006, '896999071131', '2025-09-11 12:45:01', 'used', '2025-09-11 20:45:01', '2025-09-11 20:45:01');
INSERT INTO `user_raffle_order_000` VALUES (19, 'k7znlr46', 100301, '测试活动', 100006, '539505157720', '2025-09-11 12:45:02', 'used', '2025-09-11 20:45:01', '2025-09-11 20:45:02');
INSERT INTO `user_raffle_order_000` VALUES (20, 'p1pvgw.fkp22', 100301, '测试活动', 100006, '927384341281', '2025-09-11 12:45:03', 'used', '2025-09-11 20:45:03', '2025-09-11 20:45:03');
INSERT INTO `user_raffle_order_000` VALUES (21, 'i1csbt70', 100301, '测试活动', 100006, '561507325512', '2025-09-11 12:45:04', 'used', '2025-09-11 20:45:04', '2025-09-11 20:45:04');
INSERT INTO `user_raffle_order_000` VALUES (22, 'lh7lnf.klt77', 100301, '测试活动', 100006, '059644126934', '2025-09-11 12:45:09', 'used', '2025-09-11 20:45:09', '2025-09-11 20:45:09');
INSERT INTO `user_raffle_order_000` VALUES (23, 'jwui2v_jjl', 100301, '测试活动', 100006, '585060132129', '2025-09-11 12:45:10', 'used', '2025-09-11 20:45:09', '2025-09-11 20:45:10');
INSERT INTO `user_raffle_order_000` VALUES (24, 'ttbtw9.j2h', 100301, '测试活动', 100006, '002698604461', '2025-09-11 12:45:24', 'used', '2025-09-11 20:45:23', '2025-09-11 20:45:24');
INSERT INTO `user_raffle_order_000` VALUES (25, 'izhpd6_ffx11', 100301, '测试活动', 100006, '261541472199', '2025-09-11 12:45:28', 'used', '2025-09-11 20:45:27', '2025-09-11 20:45:28');
INSERT INTO `user_raffle_order_000` VALUES (26, 'o97fmn35', 100301, '测试活动', 100006, '822805218161', '2025-09-11 12:45:35', 'used', '2025-09-11 20:45:35', '2025-09-11 20:45:35');
INSERT INTO `user_raffle_order_000` VALUES (27, 'pu9ggq_tw253', 100301, '测试活动', 100006, '908298497356', '2025-09-11 12:45:38', 'used', '2025-09-11 20:45:38', '2025-09-11 20:45:38');
INSERT INTO `user_raffle_order_000` VALUES (28, 'hm1nuy.gi196', 100301, '测试活动', 100006, '657132214050', '2025-09-11 12:45:40', 'used', '2025-09-11 20:45:40', '2025-09-11 20:45:40');
INSERT INTO `user_raffle_order_000` VALUES (29, 'k18to3_p0z', 100301, '测试活动', 100006, '264946207357', '2025-09-11 12:45:45', 'used', '2025-09-11 20:45:45', '2025-09-11 20:45:45');
INSERT INTO `user_raffle_order_000` VALUES (30, 'tbfv9r.lpvfim97', 100301, '测试活动', 100006, '499739330647', '2025-09-11 12:45:46', 'used', '2025-09-11 20:45:46', '2025-09-11 20:45:46');
INSERT INTO `user_raffle_order_000` VALUES (31, 'mawnuy_nh7', 100301, '测试活动', 100006, '528496494899', '2025-09-11 12:45:49', 'used', '2025-09-11 20:45:49', '2025-09-11 20:45:49');
INSERT INTO `user_raffle_order_000` VALUES (32, 'i1cvdk.k7f66', 100301, '测试活动', 100006, '399069982632', '2025-09-11 12:45:52', 'used', '2025-09-11 20:45:52', '2025-09-11 20:45:52');
INSERT INTO `user_raffle_order_000` VALUES (33, 'lmfmhh.k2j73', 100301, '测试活动', 100006, '831687428725', '2025-09-11 12:45:54', 'used', '2025-09-11 20:45:54', '2025-09-11 20:45:54');
INSERT INTO `user_raffle_order_000` VALUES (34, 'is8k1z_pvp32', 100301, '测试活动', 100006, '663204164119', '2025-09-11 12:45:56', 'used', '2025-09-11 20:45:56', '2025-09-11 20:45:56');
INSERT INTO `user_raffle_order_000` VALUES (35, 'lzbmd2_u4x', 100301, '测试活动', 100006, '168815378322', '2025-09-11 12:45:58', 'used', '2025-09-11 20:45:58', '2025-09-11 20:45:58');
INSERT INTO `user_raffle_order_000` VALUES (36, 'nlrlqi16', 100301, '测试活动', 100006, '609335705023', '2025-09-11 12:46:00', 'used', '2025-09-11 20:46:00', '2025-09-11 20:46:00');
INSERT INTO `user_raffle_order_000` VALUES (37, 'mawgge_uws', 100301, '测试活动', 100006, '961520270278', '2025-09-11 12:46:02', 'used', '2025-09-11 20:46:02', '2025-09-11 20:46:02');
INSERT INTO `user_raffle_order_000` VALUES (38, 'my230', 100301, '测试活动', 100006, '440937137962', '2025-09-11 12:46:09', 'used', '2025-09-11 20:46:09', '2025-09-11 20:46:09');
INSERT INTO `user_raffle_order_000` VALUES (39, 'k5qsbt_o3y67', 100301, '测试活动', 100006, '577835539649', '2025-09-11 12:46:14', 'used', '2025-09-11 20:46:13', '2025-09-11 20:46:14');
INSERT INTO `user_raffle_order_000` VALUES (40, 'lm1sbt.tcu', 100301, '测试活动', 100006, '955161245416', '2025-09-11 12:46:16', 'used', '2025-09-11 20:46:16', '2025-09-11 20:46:16');
INSERT INTO `user_raffle_order_000` VALUES (41, 'rmftt4.k0m', 100301, '测试活动', 100006, '088093248433', '2025-09-11 12:46:18', 'used', '2025-09-11 20:46:18', '2025-09-11 20:46:18');
INSERT INTO `user_raffle_order_000` VALUES (42, 'fvpts4_om789', 100301, '测试活动', 100006, '190758392449', '2025-09-11 12:46:25', 'used', '2025-09-11 20:46:24', '2025-09-11 20:46:25');
INSERT INTO `user_raffle_order_000` VALUES (43, 'ggqlnf_fo574', 100301, '测试活动', 100006, '034820604596', '2025-09-11 12:46:26', 'used', '2025-09-11 20:46:26', '2025-09-11 20:46:26');
INSERT INTO `user_raffle_order_000` VALUES (44, 'i1clo517', 100301, '测试活动', 100006, '274326223439', '2025-09-11 12:46:27', 'used', '2025-09-11 20:46:27', '2025-09-11 20:46:27');
INSERT INTO `user_raffle_order_000` VALUES (45, 'lm1sds_gii61', 100301, '测试活动', 100006, '888275641024', '2025-09-11 12:46:28', 'used', '2025-09-11 20:46:28', '2025-09-11 20:46:28');
INSERT INTO `user_raffle_order_000` VALUES (46, 'tbfv9r91', 100301, '测试活动', 100006, '755595728593', '2025-09-11 12:46:30', 'used', '2025-09-11 20:46:29', '2025-09-11 20:46:30');
INSERT INTO `user_raffle_order_000` VALUES (47, 'gncsbt51', 100301, '测试活动', 100006, '720279080918', '2025-09-11 12:46:31', 'used', '2025-09-11 20:46:31', '2025-09-11 20:46:31');
INSERT INTO `user_raffle_order_000` VALUES (48, 'gs2k34.fgw', 100301, '测试活动', 100006, '789544422535', '2025-09-11 12:46:36', 'used', '2025-09-11 20:46:36', '2025-09-11 20:46:36');
INSERT INTO `user_raffle_order_000` VALUES (49, 'k5qgs270', 100301, '测试活动', 100006, '163734336600', '2025-09-11 12:46:39', 'used', '2025-09-11 20:46:39', '2025-09-11 20:46:39');
INSERT INTO `user_raffle_order_000` VALUES (50, 'irescp.lel27', 100301, '测试活动', 100006, '496349760622', '2025-09-11 12:46:48', 'used', '2025-09-11 20:46:47', '2025-09-11 20:46:48');
INSERT INTO `user_raffle_order_000` VALUES (51, 'fmnrmi35', 100301, '测试活动', 100006, '930968326245', '2025-09-11 12:46:53', 'used', '2025-09-11 20:46:53', '2025-09-11 20:46:54');
INSERT INTO `user_raffle_order_000` VALUES (52, 'k8qkfk82', 100301, '测试活动', 100006, '727847101472', '2025-09-11 12:46:59', 'used', '2025-09-11 20:46:59', '2025-09-11 20:46:59');
INSERT INTO `user_raffle_order_000` VALUES (53, 'irescp_gl4o6y', 100301, '测试活动', 100006, '454556556132', '2025-09-11 12:47:01', 'used', '2025-09-11 20:47:01', '2025-09-11 20:47:01');
INSERT INTO `user_raffle_order_000` VALUES (54, 'n0emzg_tog', 100301, '测试活动', 100006, '576059853319', '2025-09-11 12:47:09', 'used', '2025-09-11 20:47:08', '2025-09-11 20:47:08');
INSERT INTO `user_raffle_order_000` VALUES (55, 'me4hce.sld', 100301, '测试活动', 100006, '636778115994', '2025-09-11 12:47:12', 'used', '2025-09-11 20:47:11', '2025-09-11 20:47:12');
INSERT INTO `user_raffle_order_000` VALUES (56, 'fozmy2_o9x53', 100301, '测试活动', 100006, '058878789987', '2025-09-11 12:47:14', 'used', '2025-09-11 20:47:14', '2025-09-11 20:47:14');
INSERT INTO `user_raffle_order_000` VALUES (57, 'izhlq416', 100301, '测试活动', 100006, '919634110683', '2025-09-11 12:47:16', 'used', '2025-09-11 20:47:16', '2025-09-11 20:47:16');
INSERT INTO `user_raffle_order_000` VALUES (58, 'fmnlj154', 100301, '测试活动', 100006, '889895918543', '2025-09-11 12:47:17', 'used', '2025-09-11 20:47:17', '2025-09-11 20:47:17');
INSERT INTO `user_raffle_order_000` VALUES (59, 'tbfv9r.ul4', 100301, '测试活动', 100006, '511366015638', '2025-09-11 12:47:18', 'used', '2025-09-11 20:47:18', '2025-09-11 20:47:18');

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
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户抽奖订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_raffle_order_001
-- ----------------------------
INSERT INTO `user_raffle_order_001` VALUES (1, 'mg0iuc15', 100301, '测试活动', 100006, '751045921382', '2025-09-11 12:43:56', 'used', '2025-09-11 20:43:56', '2025-09-11 20:43:56');
INSERT INTO `user_raffle_order_001` VALUES (2, 'o97fmn_q0b37', 100301, '测试活动', 100006, '714351845530', '2025-09-11 12:44:03', 'used', '2025-09-11 20:44:03', '2025-09-11 20:44:03');
INSERT INTO `user_raffle_order_001` VALUES (3, 'gztmft40', 100301, '测试活动', 100006, '196080466656', '2025-09-11 12:44:04', 'used', '2025-09-11 20:44:04', '2025-09-11 20:44:04');
INSERT INTO `user_raffle_order_001` VALUES (4, 'o97kgs32', 100301, '测试活动', 100006, '712724533387', '2025-09-11 12:44:07', 'used', '2025-09-11 20:44:06', '2025-09-11 20:44:07');
INSERT INTO `user_raffle_order_001` VALUES (5, 'izhlu897', 100301, '测试活动', 100006, '643627510170', '2025-09-11 12:44:09', 'used', '2025-09-11 20:44:09', '2025-09-11 20:44:09');
INSERT INTO `user_raffle_order_001` VALUES (6, 'ggqk1z.ijv31', 100301, '测试活动', 100006, '244288706341', '2025-09-11 12:44:11', 'used', '2025-09-11 20:44:11', '2025-09-11 20:44:11');
INSERT INTO `user_raffle_order_001` VALUES (7, 'freg6n24', 100301, '测试活动', 100006, '599728785608', '2025-09-11 12:44:12', 'used', '2025-09-11 20:44:12', '2025-09-11 20:44:12');
INSERT INTO `user_raffle_order_001` VALUES (8, 'feog2w_pet4', 100301, '测试活动', 100006, '049233571966', '2025-09-11 12:44:23', 'used', '2025-09-11 20:44:22', '2025-09-11 20:44:23');
INSERT INTO `user_raffle_order_001` VALUES (9, 'tbfnuy.o8678', 100301, '测试活动', 100006, '468320298081', '2025-09-11 12:44:26', 'used', '2025-09-11 20:44:26', '2025-09-11 20:44:26');
INSERT INTO `user_raffle_order_001` VALUES (10, 'ui8_noz35', 100301, '测试活动', 100006, '160126513297', '2025-09-11 12:44:32', 'used', '2025-09-11 20:44:32', '2025-09-11 20:44:32');
INSERT INTO `user_raffle_order_001` VALUES (11, 'ts5hvb63', 100301, '测试活动', 100006, '611277238666', '2025-09-11 12:44:36', 'used', '2025-09-11 20:44:36', '2025-09-11 20:44:36');
INSERT INTO `user_raffle_order_001` VALUES (12, 'ixjit6_tom', 100301, '测试活动', 100006, '838603739738', '2025-09-11 12:44:40', 'used', '2025-09-11 20:44:39', '2025-09-11 20:44:40');
INSERT INTO `user_raffle_order_001` VALUES (13, 'fevfoz_pca37', 100301, '测试活动', 100006, '494353821056', '2025-09-11 12:44:41', 'used', '2025-09-11 20:44:40', '2025-09-11 20:44:40');
INSERT INTO `user_raffle_order_001` VALUES (14, 'lm1sbt53', 100301, '测试活动', 100006, '264201005141', '2025-09-11 12:44:51', 'used', '2025-09-11 20:44:51', '2025-09-11 20:44:51');
INSERT INTO `user_raffle_order_001` VALUES (15, 'i1cvdk30', 100301, '测试活动', 100006, '556689350107', '2025-09-11 12:44:54', 'used', '2025-09-11 20:44:53', '2025-09-11 20:44:54');
INSERT INTO `user_raffle_order_001` VALUES (16, 'irescp_lef33', 100301, '测试活动', 100006, '090265060035', '2025-09-11 12:44:55', 'used', '2025-09-11 20:44:54', '2025-09-11 20:44:55');
INSERT INTO `user_raffle_order_001` VALUES (17, 'oq3kgn_i6780', 100301, '测试活动', 100006, '491662474781', '2025-09-11 12:44:58', 'used', '2025-09-11 20:44:57', '2025-09-11 20:44:58');
INSERT INTO `user_raffle_order_001` VALUES (18, 'i1cuji.kdw21', 100301, '测试活动', 100006, '131812513921', '2025-09-11 12:44:59', 'used', '2025-09-11 20:44:59', '2025-09-11 20:44:59');
INSERT INTO `user_raffle_order_001` VALUES (19, 'ryiiuk.ml733', 100301, '测试活动', 100006, '720537053052', '2025-09-11 12:45:05', 'used', '2025-09-11 20:45:05', '2025-09-11 20:45:05');
INSERT INTO `user_raffle_order_001` VALUES (20, 'irescp.fo890', 100301, '测试活动', 100006, '548845045615', '2025-09-11 12:45:06', 'used', '2025-09-11 20:45:05', '2025-09-11 20:45:06');
INSERT INTO `user_raffle_order_001` VALUES (21, 'mzitue_jql', 100301, '测试活动', 100006, '052226621174', '2025-09-11 12:45:06', 'used', '2025-09-11 20:45:06', '2025-09-11 20:45:06');
INSERT INTO `user_raffle_order_001` VALUES (22, 'l7nrqi_hrg60', 100301, '测试活动', 100006, '053397780277', '2025-09-11 12:45:07', 'used', '2025-09-11 20:45:07', '2025-09-11 20:45:07');
INSERT INTO `user_raffle_order_001` VALUES (23, 'ts5tbf_rvh19', 100301, '测试活动', 100006, '205160331391', '2025-09-11 12:45:11', 'used', '2025-09-11 20:45:11', '2025-09-11 20:45:11');
INSERT INTO `user_raffle_order_001` VALUES (24, 'lm1i2v6', 100301, '测试活动', 100006, '160548526180', '2025-09-11 12:45:12', 'used', '2025-09-11 20:45:12', '2025-09-11 20:45:12');
INSERT INTO `user_raffle_order_001` VALUES (25, 'ryiiuk.mlb49', 100301, '测试活动', 100006, '376403963358', '2025-09-11 12:45:13', 'used', '2025-09-11 20:45:12', '2025-09-11 20:45:13');
INSERT INTO `user_raffle_order_001` VALUES (26, 'tgmo3v_jd4', 100301, '测试活动', 100006, '374338139968', '2025-09-11 12:45:14', 'used', '2025-09-11 20:45:13', '2025-09-11 20:45:14');
INSERT INTO `user_raffle_order_001` VALUES (27, 'i1cvdk27', 100301, '测试活动', 100006, '969079391077', '2025-09-11 12:45:14', 'used', '2025-09-11 20:45:14', '2025-09-11 20:45:14');
INSERT INTO `user_raffle_order_001` VALUES (28, 'ggqlnf69', 100301, '测试活动', 100006, '305883833827', '2025-09-11 12:45:19', 'used', '2025-09-11 20:45:19', '2025-09-11 20:45:19');
INSERT INTO `user_raffle_order_001` VALUES (29, 'jwui2v10', 100301, '测试活动', 100006, '634231351568', '2025-09-11 12:45:22', 'used', '2025-09-11 20:45:22', '2025-09-11 20:45:22');
INSERT INTO `user_raffle_order_001` VALUES (30, 'k1zk5m10', 100301, '测试活动', 100006, '703936929763', '2025-09-11 12:45:23', 'used', '2025-09-11 20:45:22', '2025-09-11 20:45:23');
INSERT INTO `user_raffle_order_001` VALUES (31, 'tw9izl.n4v31', 100301, '测试活动', 100006, '972645872059', '2025-09-11 12:45:24', 'used', '2025-09-11 20:45:24', '2025-09-11 20:45:24');
INSERT INTO `user_raffle_order_001` VALUES (32, 'pu9ggq.lfs39', 100301, '测试活动', 100006, '860121831868', '2025-09-11 12:45:25', 'used', '2025-09-11 20:45:25', '2025-09-11 20:45:25');
INSERT INTO `user_raffle_order_001` VALUES (33, 'i1cvdk63', 100301, '测试活动', 100006, '281965061322', '2025-09-11 12:45:40', 'used', '2025-09-11 20:45:39', '2025-09-11 20:45:40');
INSERT INTO `user_raffle_order_001` VALUES (34, 'i1cl5v_fs1', 100301, '测试活动', 100006, '279074109023', '2025-09-11 12:45:41', 'used', '2025-09-11 20:45:41', '2025-09-11 20:45:41');
INSERT INTO `user_raffle_order_001` VALUES (35, 'hm1i2v_kkh', 100301, '测试活动', 100006, '727261148286', '2025-09-11 12:45:48', 'used', '2025-09-11 20:45:48', '2025-09-11 20:45:48');
INSERT INTO `user_raffle_order_001` VALUES (36, 'vacsbt43', 100301, '测试活动', 100006, '089917255530', '2025-09-11 12:45:50', 'used', '2025-09-11 20:45:50', '2025-09-11 20:45:50');
INSERT INTO `user_raffle_order_001` VALUES (37, 'k88to374', 100301, '测试活动', 100006, '235274431821', '2025-09-11 12:45:52', 'used', '2025-09-11 20:45:51', '2025-09-11 20:45:51');
INSERT INTO `user_raffle_order_001` VALUES (38, 'lmfmhh_rwe', 100301, '测试活动', 100006, '292703443524', '2025-09-11 12:45:53', 'used', '2025-09-11 20:45:53', '2025-09-11 20:45:53');
INSERT INTO `user_raffle_order_001` VALUES (39, 'ob5j6i.ik1', 100301, '测试活动', 100006, '882857075127', '2025-09-11 12:45:55', 'used', '2025-09-11 20:45:54', '2025-09-11 20:45:55');
INSERT INTO `user_raffle_order_001` VALUES (40, 'o97sbt64', 100301, '测试活动', 100006, '754661956688', '2025-09-11 12:45:57', 'used', '2025-09-11 20:45:57', '2025-09-11 20:45:57');
INSERT INTO `user_raffle_order_001` VALUES (41, 'gs2k34.qx879', 100301, '测试活动', 100006, '886237624534', '2025-09-11 12:45:59', 'used', '2025-09-11 20:45:58', '2025-09-11 20:45:59');
INSERT INTO `user_raffle_order_001` VALUES (42, 'tgmo3v.l7p33', 100301, '测试活动', 100006, '421460337531', '2025-09-11 12:46:04', 'used', '2025-09-11 20:46:03', '2025-09-11 20:46:04');
INSERT INTO `user_raffle_order_001` VALUES (43, 'k7nggq23', 100301, '测试活动', 100006, '077860360760', '2025-09-11 12:46:04', 'used', '2025-09-11 20:46:04', '2025-09-11 20:46:04');
INSERT INTO `user_raffle_order_001` VALUES (44, 'mg0iuc_nky', 100301, '测试活动', 100006, '096264337911', '2025-09-11 12:46:08', 'used', '2025-09-11 20:46:08', '2025-09-11 20:46:08');
INSERT INTO `user_raffle_order_001` VALUES (45, 'p1pscp68', 100301, '测试活动', 100006, '211123330529', '2025-09-11 12:46:11', 'used', '2025-09-11 20:46:11', '2025-09-11 20:46:11');
INSERT INTO `user_raffle_order_001` VALUES (46, 'lnf59', 100301, '测试活动', 100006, '018577031644', '2025-09-11 12:46:12', 'used', '2025-09-11 20:46:12', '2025-09-11 20:46:12');
INSERT INTO `user_raffle_order_001` VALUES (47, 'ffxlmf.puf58', 100301, '测试活动', 100006, '201073428390', '2025-09-11 12:46:13', 'used', '2025-09-11 20:46:12', '2025-09-11 20:46:13');
INSERT INTO `user_raffle_order_001` VALUES (48, 'sf0ppm_gl296', 100301, '测试活动', 100006, '983424474378', '2025-09-11 12:46:14', 'used', '2025-09-11 20:46:14', '2025-09-11 20:46:14');
INSERT INTO `user_raffle_order_001` VALUES (49, 'izhsf0.ka036', 100301, '测试活动', 100006, '269441241708', '2025-09-11 12:46:20', 'used', '2025-09-11 20:46:20', '2025-09-11 20:46:20');
INSERT INTO `user_raffle_order_001` VALUES (50, 'o97kgs_rw844', 100301, '测试活动', 100006, '967736944185', '2025-09-11 12:46:24', 'used', '2025-09-11 20:46:24', '2025-09-11 20:46:24');
INSERT INTO `user_raffle_order_001` VALUES (51, 'gncsbt25', 100301, '测试活动', 100006, '049427887063', '2025-09-11 12:46:31', 'used', '2025-09-11 20:46:30', '2025-09-11 20:46:31');
INSERT INTO `user_raffle_order_001` VALUES (52, 'hm1p8q.mtn', 100301, '测试活动', 100006, '899656495241', '2025-09-11 12:46:32', 'used', '2025-09-11 20:46:32', '2025-09-11 20:46:32');
INSERT INTO `user_raffle_order_001` VALUES (53, 'v9rlnf92', 100301, '测试活动', 100006, '373505928385', '2025-09-11 12:46:38', 'used', '2025-09-11 20:46:37', '2025-09-11 20:46:38');
INSERT INTO `user_raffle_order_001` VALUES (54, 'mawgge_itu', 100301, '测试活动', 100006, '502767082134', '2025-09-11 12:46:39', 'used', '2025-09-11 20:46:38', '2025-09-11 20:46:39');
INSERT INTO `user_raffle_order_001` VALUES (55, 'uclgm3_hs4', 100301, '测试活动', 100006, '755330709430', '2025-09-11 12:46:44', 'used', '2025-09-11 20:46:43', '2025-09-11 20:46:44');
INSERT INTO `user_raffle_order_001` VALUES (56, 'p1pscp_fo50', 100301, '测试活动', 100006, '804330544240', '2025-09-11 12:46:47', 'used', '2025-09-11 20:46:47', '2025-09-11 20:46:47');
INSERT INTO `user_raffle_order_001` VALUES (57, 'jlbffg.nky48', 100301, '测试活动', 100006, '576425970003', '2025-09-11 12:46:52', 'used', '2025-09-11 20:46:51', '2025-09-11 20:46:52');
INSERT INTO `user_raffle_order_001` VALUES (58, 'k5qgs271', 100301, '测试活动', 100006, '091079022426', '2025-09-11 12:46:56', 'used', '2025-09-11 20:46:56', '2025-09-11 20:46:56');
INSERT INTO `user_raffle_order_001` VALUES (59, 'k5mmd2.fg017', 100301, '测试活动', 100006, '542194017491', '2025-09-11 12:46:57', 'used', '2025-09-11 20:46:57', '2025-09-11 20:46:57');
INSERT INTO `user_raffle_order_001` VALUES (60, 'mg0iuc26', 100301, '测试活动', 100006, '564280232468', '2025-09-11 12:46:58', 'used', '2025-09-11 20:46:58', '2025-09-11 20:46:58');
INSERT INTO `user_raffle_order_001` VALUES (61, 'l7nrqi26', 100301, '测试活动', 100006, '735090489834', '2025-09-11 12:47:00', 'used', '2025-09-11 20:46:59', '2025-09-11 20:47:00');
INSERT INTO `user_raffle_order_001` VALUES (62, 'fevfoz96', 100301, '测试活动', 100006, '192333842642', '2025-09-11 12:47:04', 'used', '2025-09-11 20:47:04', '2025-09-11 20:47:04');
INSERT INTO `user_raffle_order_001` VALUES (63, 'v9rmdm_fevfrz12', 100301, '测试活动', 100006, '225486083838', '2025-09-11 12:47:06', 'used', '2025-09-11 20:47:06', '2025-09-11 20:47:06');
INSERT INTO `user_raffle_order_001` VALUES (64, 'tt6kfy_nyg', 100301, '测试活动', 100006, '974224320960', '2025-09-11 12:47:13', 'used', '2025-09-11 20:47:13', '2025-09-11 20:47:13');
INSERT INTO `user_raffle_order_001` VALUES (65, 'fextfv63', 100301, '测试活动', 100006, '323297045715', '2025-09-11 12:47:19', 'used', '2025-09-11 20:47:19', '2025-09-11 20:47:19');
INSERT INTO `user_raffle_order_001` VALUES (66, 'jlbffg92', 100301, '测试活动', 100006, '517098628988', '2025-09-11 12:47:21', 'used', '2025-09-11 20:47:20', '2025-09-11 20:47:21');

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
) ENGINE = InnoDB AUTO_INCREMENT = 62 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户抽奖订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_raffle_order_002
-- ----------------------------
INSERT INTO `user_raffle_order_002` VALUES (1, 'v9blnf.uc9', 100301, '测试活动', 100006, '101549619845', '2025-09-11 12:44:02', 'used', '2025-09-11 20:44:01', '2025-09-11 20:44:02');
INSERT INTO `user_raffle_order_002` VALUES (2, 'jlbffg.tu527', 100301, '测试活动', 100006, '591801031170', '2025-09-11 12:44:08', 'used', '2025-09-11 20:44:08', '2025-09-11 20:44:08');
INSERT INTO `user_raffle_order_002` VALUES (3, 'ryiiuk88', 100301, '测试活动', 100006, '673171703022', '2025-09-11 12:44:14', 'used', '2025-09-11 20:44:13', '2025-09-11 20:44:14');
INSERT INTO `user_raffle_order_002` VALUES (4, 'me4nzu4', 100301, '测试活动', 100006, '168450943720', '2025-09-11 12:44:15', 'used', '2025-09-11 20:44:15', '2025-09-11 20:44:15');
INSERT INTO `user_raffle_order_002` VALUES (5, 'lmfmhh36', 100301, '测试活动', 100006, '275814493901', '2025-09-11 12:44:20', 'used', '2025-09-11 20:44:19', '2025-09-11 20:44:20');
INSERT INTO `user_raffle_order_002` VALUES (6, 'm97k5q34', 100301, '测试活动', 100006, '532317029989', '2025-09-11 12:44:21', 'used', '2025-09-11 20:44:20', '2025-09-11 20:44:21');
INSERT INTO `user_raffle_order_002` VALUES (7, 'fvpki783', 100301, '测试活动', 100006, '226303920407', '2025-09-11 12:44:21', 'used', '2025-09-11 20:44:21', '2025-09-11 20:44:22');
INSERT INTO `user_raffle_order_002` VALUES (8, 'tbfnuy_gcf', 100301, '测试活动', 100006, '763544290330', '2025-09-11 12:44:24', 'used', '2025-09-11 20:44:24', '2025-09-11 20:44:24');
INSERT INTO `user_raffle_order_002` VALUES (9, 'ts5hvb.oti', 100301, '测试活动', 100006, '858516683395', '2025-09-11 12:44:30', 'used', '2025-09-11 20:44:30', '2025-09-11 20:44:30');
INSERT INTO `user_raffle_order_002` VALUES (10, 'izhsf0.nky82', 100301, '测试活动', 100006, '760840137427', '2025-09-11 12:44:34', 'used', '2025-09-11 20:44:34', '2025-09-11 20:44:34');
INSERT INTO `user_raffle_order_002` VALUES (11, 'lmfmhh_k28', 100301, '测试活动', 100006, '526427902784', '2025-09-11 12:44:36', 'used', '2025-09-11 20:44:35', '2025-09-11 20:44:36');
INSERT INTO `user_raffle_order_002` VALUES (12, 'n0emzg_q0b61', 100301, '测试活动', 100006, '014082357956', '2025-09-11 12:44:39', 'used', '2025-09-11 20:44:39', '2025-09-11 20:44:39');
INSERT INTO `user_raffle_order_002` VALUES (13, 'v9bmdm_u0617', 100301, '测试活动', 100006, '877777181543', '2025-09-11 12:44:47', 'used', '2025-09-11 20:44:47', '2025-09-11 20:44:47');
INSERT INTO `user_raffle_order_002` VALUES (14, 'gncsbt31', 100301, '测试活动', 100006, '004062557558', '2025-09-11 12:44:48', 'used', '2025-09-11 20:44:48', '2025-09-11 20:44:48');
INSERT INTO `user_raffle_order_002` VALUES (15, 'i1clo591', 100301, '测试活动', 100006, '036385657654', '2025-09-11 12:44:52', 'used', '2025-09-11 20:44:52', '2025-09-11 20:44:52');
INSERT INTO `user_raffle_order_002` VALUES (16, 'i1csbt_n5f', 100301, '测试活动', 100006, '608949369459', '2025-09-11 12:44:55', 'used', '2025-09-11 20:44:55', '2025-09-11 20:44:55');
INSERT INTO `user_raffle_order_002` VALUES (17, 'lh7lnf87', 100301, '测试活动', 100006, '074459947055', '2025-09-11 12:44:57', 'used', '2025-09-11 20:44:57', '2025-09-11 20:44:57');
INSERT INTO `user_raffle_order_002` VALUES (18, 'o97scp.gp8', 100301, '测试活动', 100006, '224063784769', '2025-09-11 12:44:58', 'used', '2025-09-11 20:44:58', '2025-09-11 20:44:58');
INSERT INTO `user_raffle_order_002` VALUES (19, 'ob5j6i22', 100301, '测试活动', 100006, '428235789937', '2025-09-11 12:45:16', 'used', '2025-09-11 20:45:16', '2025-09-11 20:45:16');
INSERT INTO `user_raffle_order_002` VALUES (20, 'ktzmzi69', 100301, '测试活动', 100006, '181300844986', '2025-09-11 12:45:18', 'used', '2025-09-11 20:45:17', '2025-09-11 20:45:18');
INSERT INTO `user_raffle_order_002` VALUES (21, 'lm1md2_ggn63', 100301, '测试活动', 100006, '846933349535', '2025-09-11 12:45:21', 'used', '2025-09-11 20:45:21', '2025-09-11 20:45:21');
INSERT INTO `user_raffle_order_002` VALUES (22, 'v9rmdm84', 100301, '测试活动', 100006, '062273437164', '2025-09-11 12:45:26', 'used', '2025-09-11 20:45:26', '2025-09-11 20:45:26');
INSERT INTO `user_raffle_order_002` VALUES (23, 'ttbtw9.i2vk1z', 100301, '测试活动', 100006, '697345318013', '2025-09-11 12:45:28', 'used', '2025-09-11 20:45:28', '2025-09-11 20:45:28');
INSERT INTO `user_raffle_order_002` VALUES (24, 'tbfv9r75', 100301, '测试活动', 100006, '519359162157', '2025-09-11 12:45:34', 'used', '2025-09-11 20:45:34', '2025-09-11 20:45:34');
INSERT INTO `user_raffle_order_002` VALUES (25, 'k7nggq_gc3', 100301, '测试活动', 100006, '636417988062', '2025-09-11 12:45:36', 'used', '2025-09-11 20:45:36', '2025-09-11 20:45:36');
INSERT INTO `user_raffle_order_002` VALUES (26, 'kgs.fh847', 100301, '测试活动', 100006, '669646618798', '2025-09-11 12:45:42', 'used', '2025-09-11 20:45:42', '2025-09-11 20:45:42');
INSERT INTO `user_raffle_order_002` VALUES (27, 'freg6n_k1z', 100301, '测试活动', 100006, '021044035223', '2025-09-11 12:45:43', 'used', '2025-09-11 20:45:43', '2025-09-11 20:45:43');
INSERT INTO `user_raffle_order_002` VALUES (28, 'k1zk5m_n54', 100301, '测试活动', 100006, '466909407992', '2025-09-11 12:45:44', 'used', '2025-09-11 20:45:44', '2025-09-11 20:45:44');
INSERT INTO `user_raffle_order_002` VALUES (29, 'pu9ggq38', 100301, '测试活动', 100006, '710323640113', '2025-09-11 12:45:47', 'used', '2025-09-11 20:45:46', '2025-09-11 20:45:47');
INSERT INTO `user_raffle_order_002` VALUES (30, 'o97sbt16', 100301, '测试活动', 100006, '681239720515', '2025-09-11 12:45:51', 'used', '2025-09-11 20:45:50', '2025-09-11 20:45:51');
INSERT INTO `user_raffle_order_002` VALUES (31, 'hm1i2v66', 100301, '测试活动', 100006, '566605609703', '2025-09-11 12:45:56', 'used', '2025-09-11 20:45:55', '2025-09-11 20:45:55');
INSERT INTO `user_raffle_order_002` VALUES (32, 'p1pscp87', 100301, '测试活动', 100006, '225138803660', '2025-09-11 12:45:59', 'used', '2025-09-11 20:45:59', '2025-09-11 20:45:59');
INSERT INTO `user_raffle_order_002` VALUES (33, 'i1crqi.i78', 100301, '测试活动', 100006, '943723262091', '2025-09-11 12:46:03', 'used', '2025-09-11 20:46:02', '2025-09-11 20:46:03');
INSERT INTO `user_raffle_order_002` VALUES (34, 'hm1nuy_n5c', 100301, '测试活动', 100006, '736193297878', '2025-09-11 12:46:05', 'used', '2025-09-11 20:46:05', '2025-09-11 20:46:05');
INSERT INTO `user_raffle_order_002` VALUES (35, 'gs2m5617', 100301, '测试活动', 100006, '019337974980', '2025-09-11 12:46:07', 'used', '2025-09-11 20:46:06', '2025-09-11 20:46:07');
INSERT INTO `user_raffle_order_002` VALUES (36, 'ggqryd.p8h32', 100301, '测试活动', 100006, '638983123912', '2025-09-11 12:46:15', 'used', '2025-09-11 20:46:15', '2025-09-11 20:46:15');
INSERT INTO `user_raffle_order_002` VALUES (37, 'izhfoz.r5v', 100301, '测试活动', 100006, '513263877014', '2025-09-11 12:46:17', 'used', '2025-09-11 20:46:17', '2025-09-11 20:46:17');
INSERT INTO `user_raffle_order_002` VALUES (38, 'tw9izl_h9612', 100301, '测试活动', 100006, '452489702941', '2025-09-11 12:46:18', 'used', '2025-09-11 20:46:17', '2025-09-11 20:46:18');
INSERT INTO `user_raffle_order_002` VALUES (39, 'i1clo5_ny282', 100301, '测试活动', 100006, '628279572336', '2025-09-11 12:46:19', 'used', '2025-09-11 20:46:19', '2025-09-11 20:46:19');
INSERT INTO `user_raffle_order_002` VALUES (40, 'tbfnuy.fof31', 100301, '测试活动', 100006, '145467791412', '2025-09-11 12:46:34', 'used', '2025-09-11 20:46:33', '2025-09-11 20:46:34');
INSERT INTO `user_raffle_order_002` VALUES (41, 'ob5j6i.vcr10', 100301, '测试活动', 100006, '703373154788', '2025-09-11 12:46:35', 'used', '2025-09-11 20:46:34', '2025-09-11 20:46:35');
INSERT INTO `user_raffle_order_002` VALUES (42, 'k1zu0c.fgs', 100301, '测试活动', 100006, '164211006185', '2025-09-11 12:46:37', 'used', '2025-09-11 20:46:37', '2025-09-11 20:46:37');
INSERT INTO `user_raffle_order_002` VALUES (43, 'jlbffg15', 100301, '测试活动', 100006, '052819646350', '2025-09-11 12:46:40', 'used', '2025-09-11 20:46:40', '2025-09-11 20:46:40');
INSERT INTO `user_raffle_order_002` VALUES (44, 'k4ti7b21', 100301, '测试活动', 100006, '287976874025', '2025-09-11 12:46:41', 'used', '2025-09-11 20:46:41', '2025-09-11 20:46:41');
INSERT INTO `user_raffle_order_002` VALUES (45, 'uclgm3.nh4', 100301, '测试活动', 100006, '700913857836', '2025-09-11 12:46:42', 'used', '2025-09-11 20:46:42', '2025-09-11 20:46:42');
INSERT INTO `user_raffle_order_002` VALUES (46, 'v9ru7274', 100301, '测试活动', 100006, '224493992388', '2025-09-11 12:46:43', 'used', '2025-09-11 20:46:42', '2025-09-11 20:46:43');
INSERT INTO `user_raffle_order_002` VALUES (47, 'v9rmdm.ui4', 100301, '测试活动', 100006, '710456864745', '2025-09-11 12:46:44', 'used', '2025-09-11 20:46:44', '2025-09-11 20:46:44');
INSERT INTO `user_raffle_order_002` VALUES (48, 'rydkjv_kjv91', 100301, '测试活动', 100006, '701788435796', '2025-09-11 12:46:45', 'used', '2025-09-11 20:46:45', '2025-09-11 20:46:45');
INSERT INTO `user_raffle_order_002` VALUES (49, 'my250', 100301, '测试活动', 100006, '095855023627', '2025-09-11 12:46:49', 'used', '2025-09-11 20:46:49', '2025-09-11 20:46:49');
INSERT INTO `user_raffle_order_002` VALUES (50, 'freui8.kgq', 100301, '测试活动', 100006, '828040489556', '2025-09-11 12:46:50', 'used', '2025-09-11 20:46:50', '2025-09-11 20:46:50');
INSERT INTO `user_raffle_order_002` VALUES (51, 'mawfmn_tm050', 100301, '测试活动', 100006, '604453012860', '2025-09-11 12:46:52', 'used', '2025-09-11 20:46:52', '2025-09-11 20:46:53');
INSERT INTO `user_raffle_order_002` VALUES (52, 'k7znlr_go8', 100301, '测试活动', 100006, '725174156490', '2025-09-11 12:46:55', 'used', '2025-09-11 20:46:55', '2025-09-11 20:46:55');
INSERT INTO `user_raffle_order_002` VALUES (53, 'k8qlqi.lld83', 100301, '测试活动', 100006, '683129960506', '2025-09-11 12:46:56', 'used', '2025-09-11 20:46:55', '2025-09-11 20:46:56');
INSERT INTO `user_raffle_order_002` VALUES (54, 'ppm_g3a46', 100301, '测试活动', 100006, '909087386821', '2025-09-11 12:47:04', 'used', '2025-09-11 20:47:03', '2025-09-11 20:47:04');
INSERT INTO `user_raffle_order_002` VALUES (55, 'tw6my2.roi', 100301, '测试活动', 100006, '822355996854', '2025-09-11 12:47:05', 'used', '2025-09-11 20:47:05', '2025-09-11 20:47:05');
INSERT INTO `user_raffle_order_002` VALUES (56, 'k7znlr48', 100301, '测试活动', 100006, '909759882751', '2025-09-11 12:47:07', 'used', '2025-09-11 20:47:07', '2025-09-11 20:47:07');
INSERT INTO `user_raffle_order_002` VALUES (57, 'ggqryd.twz81', 100301, '测试活动', 100006, '245589492695', '2025-09-11 12:47:08', 'used', '2025-09-11 20:47:07', '2025-09-11 20:47:08');
INSERT INTO `user_raffle_order_002` VALUES (58, 'p72k3d_qjm', 100301, '测试活动', 100006, '930684869207', '2025-09-11 12:47:11', 'used', '2025-09-11 20:47:11', '2025-09-11 20:47:11');
INSERT INTO `user_raffle_order_002` VALUES (59, 'pd6fvp_shb', 100301, '测试活动', 100006, '414109579844', '2025-09-11 12:47:15', 'used', '2025-09-11 20:47:15', '2025-09-11 20:47:15');
INSERT INTO `user_raffle_order_002` VALUES (60, 'gs2k345', 100301, '测试活动', 100006, '006237162453', '2025-09-11 12:47:16', 'used', '2025-09-11 20:47:15', '2025-09-11 20:47:16');
INSERT INTO `user_raffle_order_002` VALUES (61, 'lm182', 100301, '测试活动', 100006, '329980206213', '2025-09-11 12:47:20', 'used', '2025-09-11 20:47:19', '2025-09-11 20:47:20');

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
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户抽奖订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_raffle_order_003
-- ----------------------------
INSERT INTO `user_raffle_order_003` VALUES (1, 'fgpsbt_go848', 100301, '测试活动', 100006, '563332000698', '2025-09-11 12:43:55', 'used', '2025-09-11 20:43:55', '2025-09-11 20:43:55');
INSERT INTO `user_raffle_order_003` VALUES (2, 'k1zggq91', 100301, '测试活动', 100006, '903249703262', '2025-09-11 12:43:57', 'used', '2025-09-11 20:43:57', '2025-09-11 20:43:57');
INSERT INTO `user_raffle_order_003` VALUES (3, 'm8eiuk.ssq48', 100301, '测试活动', 100006, '076052722368', '2025-09-11 12:43:59', 'used', '2025-09-11 20:43:58', '2025-09-11 20:43:59');
INSERT INTO `user_raffle_order_003` VALUES (4, 'pu9ggq31', 100301, '测试活动', 100006, '473878697896', '2025-09-11 12:43:59', 'used', '2025-09-11 20:43:59', '2025-09-11 20:43:59');
INSERT INTO `user_raffle_order_003` VALUES (5, 'kgs62', 100301, '测试活动', 100006, '833715285056', '2025-09-11 12:44:03', 'used', '2025-09-11 20:44:02', '2025-09-11 20:44:03');
INSERT INTO `user_raffle_order_003` VALUES (6, 'tbfnuy.vgw', 100301, '测试活动', 100006, '842809108006', '2025-09-11 12:44:06', 'used', '2025-09-11 20:44:06', '2025-09-11 20:44:06');
INSERT INTO `user_raffle_order_003` VALUES (7, 'i2xmy2.r8j', 100301, '测试活动', 100006, '361751462619', '2025-09-11 12:44:10', 'used', '2025-09-11 20:44:10', '2025-09-11 20:44:10');
INSERT INTO `user_raffle_order_003` VALUES (8, 'o97sbt73', 100301, '测试活动', 100006, '961670415428', '2025-09-11 12:44:17', 'used', '2025-09-11 20:44:17', '2025-09-11 20:44:17');
INSERT INTO `user_raffle_order_003` VALUES (9, 'lm1sds.fevfrz', 100301, '测试活动', 100006, '807204192857', '2025-09-11 12:44:19', 'used', '2025-09-11 20:44:19', '2025-09-11 20:44:19');
INSERT INTO `user_raffle_order_003` VALUES (10, 'pa8kfk.gk458', 100301, '测试活动', 100006, '032030798498', '2025-09-11 12:44:24', 'used', '2025-09-11 20:44:23', '2025-09-11 20:44:24');
INSERT INTO `user_raffle_order_003` VALUES (11, 'i1cl5v_oa71', 100301, '测试活动', 100006, '440944749054', '2025-09-11 12:44:27', 'used', '2025-09-11 20:44:27', '2025-09-11 20:44:27');
INSERT INTO `user_raffle_order_003` VALUES (12, 'k18iui33', 100301, '测试活动', 100006, '247491259114', '2025-09-11 12:44:28', 'used', '2025-09-11 20:44:27', '2025-09-11 20:44:28');
INSERT INTO `user_raffle_order_003` VALUES (13, 'freg6n.pu571', 100301, '测试活动', 100006, '054695370315', '2025-09-11 12:44:28', 'used', '2025-09-11 20:44:28', '2025-09-11 20:44:28');
INSERT INTO `user_raffle_order_003` VALUES (14, 'k88to3_fj9', 100301, '测试活动', 100006, '275822482545', '2025-09-11 12:44:31', 'used', '2025-09-11 20:44:31', '2025-09-11 20:44:31');
INSERT INTO `user_raffle_order_003` VALUES (15, 'v9ru72.px5', 100301, '测试活动', 100006, '212127622355', '2025-09-11 12:44:35', 'used', '2025-09-11 20:44:35', '2025-09-11 20:44:35');
INSERT INTO `user_raffle_order_003` VALUES (16, 'vacsbt_fse16', 100301, '测试活动', 100006, '009927625959', '2025-09-11 12:44:37', 'used', '2025-09-11 20:44:37', '2025-09-11 20:44:37');
INSERT INTO `user_raffle_order_003` VALUES (17, 'jwui2v_g1y56', 100301, '测试活动', 100006, '586799642017', '2025-09-11 12:44:38', 'used', '2025-09-11 20:44:38', '2025-09-11 20:44:38');
INSERT INTO `user_raffle_order_003` VALUES (18, 'lh7k1z_go8', 100301, '测试活动', 100006, '952614189678', '2025-09-11 12:44:41', 'used', '2025-09-11 20:44:41', '2025-09-11 20:44:41');
INSERT INTO `user_raffle_order_003` VALUES (19, 'k8qi48_lid81', 100301, '测试活动', 100006, '936919063532', '2025-09-11 12:44:43', 'used', '2025-09-11 20:44:43', '2025-09-11 20:44:43');
INSERT INTO `user_raffle_order_003` VALUES (20, 'mawgge40', 100301, '测试活动', 100006, '234983687736', '2025-09-11 12:44:44', 'used', '2025-09-11 20:44:44', '2025-09-11 20:44:44');
INSERT INTO `user_raffle_order_003` VALUES (21, 'n0emzg_kfz25', 100301, '测试活动', 100006, '002259037063', '2025-09-11 12:44:45', 'used', '2025-09-11 20:44:44', '2025-09-11 20:44:45');
INSERT INTO `user_raffle_order_003` VALUES (22, 'ob5j6i_g7u34', 100301, '测试活动', 100006, '013367367828', '2025-09-11 12:44:46', 'used', '2025-09-11 20:44:45', '2025-09-11 20:44:46');
INSERT INTO `user_raffle_order_003` VALUES (23, 'fmni483', 100301, '测试活动', 100006, '361952775166', '2025-09-11 12:44:47', 'used', '2025-09-11 20:44:46', '2025-09-11 20:44:46');
INSERT INTO `user_raffle_order_003` VALUES (24, 'k5qscp66', 100301, '测试活动', 100006, '780419895861', '2025-09-11 12:44:51', 'used', '2025-09-11 20:44:50', '2025-09-11 20:44:51');
INSERT INTO `user_raffle_order_003` VALUES (25, 'ryilj176', 100301, '测试活动', 100006, '880908108216', '2025-09-11 12:44:53', 'used', '2025-09-11 20:44:53', '2025-09-11 20:44:53');
INSERT INTO `user_raffle_order_003` VALUES (26, 'freui869', 100301, '测试活动', 100006, '579729086379', '2025-09-11 12:45:00', 'used', '2025-09-11 20:45:00', '2025-09-11 20:45:00');
INSERT INTO `user_raffle_order_003` VALUES (27, 'fextfv_fip68', 100301, '测试活动', 100006, '714829104240', '2025-09-11 12:45:02', 'used', '2025-09-11 20:45:02', '2025-09-11 20:45:02');
INSERT INTO `user_raffle_order_003` VALUES (28, 'tgmo3v.lpi', 100301, '测试活动', 100006, '015206330380', '2025-09-11 12:45:08', 'used', '2025-09-11 20:45:08', '2025-09-11 20:45:08');
INSERT INTO `user_raffle_order_003` VALUES (29, 'p72k3d96', 100301, '测试活动', 100006, '062168416291', '2025-09-11 12:45:11', 'used', '2025-09-11 20:45:10', '2025-09-11 20:45:10');
INSERT INTO `user_raffle_order_003` VALUES (30, 'k7fv9r96', 100301, '测试活动', 100006, '192480101955', '2025-09-11 12:45:15', 'used', '2025-09-11 20:45:15', '2025-09-11 20:45:15');
INSERT INTO `user_raffle_order_003` VALUES (31, 'my2_q11', 100301, '测试活动', 100006, '903330690750', '2025-09-11 12:45:17', 'used', '2025-09-11 20:45:17', '2025-09-11 20:45:17');
INSERT INTO `user_raffle_order_003` VALUES (32, 'o97scp.gju20', 100301, '测试活动', 100006, '401414121250', '2025-09-11 12:45:19', 'used', '2025-09-11 20:45:18', '2025-09-11 20:45:19');
INSERT INTO `user_raffle_order_003` VALUES (33, 'v9bmdm3', 100301, '测试活动', 100006, '816010943288', '2025-09-11 12:45:20', 'used', '2025-09-11 20:45:20', '2025-09-11 20:45:20');
INSERT INTO `user_raffle_order_003` VALUES (34, 'k4ti7b6', 100301, '测试活动', 100006, '101706495049', '2025-09-11 12:45:27', 'used', '2025-09-11 20:45:26', '2025-09-11 20:45:27');
INSERT INTO `user_raffle_order_003` VALUES (35, 'v9ru72.kdt97', 100301, '测试活动', 100006, '662657061833', '2025-09-11 12:45:29', 'used', '2025-09-11 20:45:29', '2025-09-11 20:45:29');
INSERT INTO `user_raffle_order_003` VALUES (36, 'gncsbt.lel22', 100301, '测试活动', 100006, '131711098135', '2025-09-11 12:45:30', 'used', '2025-09-11 20:45:30', '2025-09-11 20:45:30');
INSERT INTO `user_raffle_order_003` VALUES (37, 'freg6n51', 100301, '测试活动', 100006, '268258696805', '2025-09-11 12:45:31', 'used', '2025-09-11 20:45:31', '2025-09-11 20:45:31');
INSERT INTO `user_raffle_order_003` VALUES (38, 'po4sf01', 100301, '测试活动', 100006, '014550644150', '2025-09-11 12:45:32', 'used', '2025-09-11 20:45:31', '2025-09-11 20:45:31');
INSERT INTO `user_raffle_order_003` VALUES (39, 'izhlq4.iyo', 100301, '测试活动', 100006, '913494310498', '2025-09-11 12:45:32', 'used', '2025-09-11 20:45:32', '2025-09-11 20:45:32');
INSERT INTO `user_raffle_order_003` VALUES (40, 'ffxlmf_o1p59', 100301, '测试活动', 100006, '779407547422', '2025-09-11 12:45:33', 'used', '2025-09-11 20:45:33', '2025-09-11 20:45:33');
INSERT INTO `user_raffle_order_003` VALUES (41, 'nlrlqi31', 100301, '测试活动', 100006, '256041089293', '2025-09-11 12:45:35', 'used', '2025-09-11 20:45:34', '2025-09-11 20:45:35');
INSERT INTO `user_raffle_order_003` VALUES (42, 'ucop8d83', 100301, '测试活动', 100006, '232504766036', '2025-09-11 12:45:37', 'used', '2025-09-11 20:45:37', '2025-09-11 20:45:37');
INSERT INTO `user_raffle_order_003` VALUES (43, 'i2xmy2.u6m', 100301, '测试活动', 100006, '000823273837', '2025-09-11 12:45:39', 'used', '2025-09-11 20:45:39', '2025-09-11 20:45:39');
INSERT INTO `user_raffle_order_003` VALUES (44, 'p72k3d.uc9', 100301, '测试活动', 100006, '332375558446', '2025-09-11 12:45:44', 'used', '2025-09-11 20:45:43', '2025-09-11 20:45:44');
INSERT INTO `user_raffle_order_003` VALUES (45, 'k8qlqi2', 100301, '测试活动', 100006, '968533123052', '2025-09-11 12:45:48', 'used', '2025-09-11 20:45:47', '2025-09-11 20:45:48');
INSERT INTO `user_raffle_order_003` VALUES (46, 'k5mit6.fke', 100301, '测试活动', 100006, '521314442205', '2025-09-11 12:46:01', 'used', '2025-09-11 20:46:01', '2025-09-11 20:46:01');
INSERT INTO `user_raffle_order_003` VALUES (47, 'gs2m565', 100301, '测试活动', 100006, '096775402100', '2025-09-11 12:46:06', 'used', '2025-09-11 20:46:06', '2025-09-11 20:46:06');
INSERT INTO `user_raffle_order_003` VALUES (48, 'jcvsbt.glt95', 100301, '测试活动', 100006, '727773414522', '2025-09-11 12:46:08', 'used', '2025-09-11 20:46:07', '2025-09-11 20:46:08');
INSERT INTO `user_raffle_order_003` VALUES (49, 'i1cvdk3', 100301, '测试活动', 100006, '906392245153', '2025-09-11 12:46:10', 'used', '2025-09-11 20:46:10', '2025-09-11 20:46:10');
INSERT INTO `user_raffle_order_003` VALUES (50, 'feog2w_tfa', 100301, '测试活动', 100006, '162334120495', '2025-09-11 12:46:21', 'used', '2025-09-11 20:46:21', '2025-09-11 20:46:21');
INSERT INTO `user_raffle_order_003` VALUES (51, 'fvpts463', 100301, '测试活动', 100006, '602546447780', '2025-09-11 12:46:22', 'used', '2025-09-11 20:46:21', '2025-09-11 20:46:22');
INSERT INTO `user_raffle_order_003` VALUES (52, 'ktzmzi_h96', 100301, '测试活动', 100006, '568965923397', '2025-09-11 12:46:22', 'used', '2025-09-11 20:46:22', '2025-09-11 20:46:22');
INSERT INTO `user_raffle_order_003` VALUES (53, 'kgs.ojx', 100301, '测试活动', 100006, '807637026925', '2025-09-11 12:46:23', 'used', '2025-09-11 20:46:23', '2025-09-11 20:46:23');
INSERT INTO `user_raffle_order_003` VALUES (54, 'tt6kfy.oq372', 100301, '测试活动', 100006, '244152113303', '2025-09-11 12:46:26', 'used', '2025-09-11 20:46:25', '2025-09-11 20:46:26');
INSERT INTO `user_raffle_order_003` VALUES (55, 'pu9ggq_s3t71', 100301, '测试活动', 100006, '788059486915', '2025-09-11 12:46:29', 'used', '2025-09-11 20:46:29', '2025-09-11 20:46:29');
INSERT INTO `user_raffle_order_003` VALUES (56, 'tgmo3v.ptd0', 100301, '测试活动', 100006, '874913292666', '2025-09-11 12:46:33', 'used', '2025-09-11 20:46:33', '2025-09-11 20:46:33');
INSERT INTO `user_raffle_order_003` VALUES (57, 'jlbi46.g30i1l', 100301, '测试活动', 100006, '040363070521', '2025-09-11 12:46:35', 'used', '2025-09-11 20:46:35', '2025-09-11 20:46:35');
INSERT INTO `user_raffle_order_003` VALUES (58, 'k7znlr38', 100301, '测试活动', 100006, '761270124687', '2025-09-11 12:46:46', 'used', '2025-09-11 20:46:46', '2025-09-11 20:46:46');
INSERT INTO `user_raffle_order_003` VALUES (59, 'ts5tbf43', 100301, '测试活动', 100006, '848741623647', '2025-09-11 12:46:49', 'used', '2025-09-11 20:46:48', '2025-09-11 20:46:48');
INSERT INTO `user_raffle_order_003` VALUES (60, 'lm1_sny', 100301, '测试活动', 100006, '227357598813', '2025-09-11 12:46:51', 'used', '2025-09-11 20:46:51', '2025-09-11 20:46:51');
INSERT INTO `user_raffle_order_003` VALUES (61, 'ttbtw9_k6731', 100301, '测试活动', 100006, '508237237670', '2025-09-11 12:47:00', 'used', '2025-09-11 20:47:00', '2025-09-11 20:47:00');
INSERT INTO `user_raffle_order_003` VALUES (62, 'ob5j6i_nevn5742', 100301, '测试活动', 100006, '223186507909', '2025-09-11 12:47:02', 'used', '2025-09-11 20:47:02', '2025-09-11 20:47:02');
INSERT INTO `user_raffle_order_003` VALUES (63, 'fmni4885', 100301, '测试活动', 100006, '014212327196', '2025-09-11 12:47:03', 'used', '2025-09-11 20:47:03', '2025-09-11 20:47:03');
INSERT INTO `user_raffle_order_003` VALUES (64, 'o97rmi.gbf98', 100301, '测试活动', 100006, '935817324091', '2025-09-11 12:47:09', 'used', '2025-09-11 20:47:09', '2025-09-11 20:47:09');
INSERT INTO `user_raffle_order_003` VALUES (65, 'pd6fvp_gky51', 100301, '测试活动', 100006, '029694653747', '2025-09-11 12:47:10', 'used', '2025-09-11 20:47:10', '2025-09-11 20:47:10');
INSERT INTO `user_raffle_order_003` VALUES (66, 'fmnrmi_vcr', 100301, '测试活动', 100006, '510999145303', '2025-09-11 12:47:13', 'used', '2025-09-11 20:47:12', '2025-09-11 20:47:12');

SET FOREIGN_KEY_CHECKS = 1;
