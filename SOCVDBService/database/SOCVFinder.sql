CREATE DATABASE  IF NOT EXISTS `SOCVFinder` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `SOCVFinder`;
-- MySQL dump 10.13  Distrib 5.7.9, for Win64 (x86_64)
--
-- Host: jdd.cyecqutvho4j.us-west-2.rds.amazonaws.com    Database: SOCVFinder
-- ------------------------------------------------------
-- Server version	5.6.27-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `batch`
--

DROP TABLE IF EXISTS `batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `batch` (
  `room_id` int(11) NOT NULL,
  `batch_nr` int(11) NOT NULL,
  `question_id` int(11) NOT NULL,
  `pd` tinyint(4) DEFAULT '0' COMMENT 'Possibile duplicate',
  `user_id` int(11) NOT NULL,
  `batch_date_start` int(11) NOT NULL,
  `batch_date_end` int(11) DEFAULT '0',
  `cv_count_before` int(11) DEFAULT '0',
  `cv_count_after` int(11) DEFAULT '0',
  `closed_date` int(11) DEFAULT '0',
  `closed_reason` varchar(245) DEFAULT NULL,
  PRIMARY KEY (`room_id`,`batch_nr`,`question_id`),
  KEY `batchEnd` (`batch_date_end`),
  KEY `userId` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `batch`
--

LOCK TABLES `batch` WRITE;
/*!40000 ALTER TABLE `batch` DISABLE KEYS */;
/*!40000 ALTER TABLE `batch` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifying`
--

DROP TABLE IF EXISTS `notifying`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notifying` (
  `chat_room_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `tag` varchar(45) NOT NULL,
  `pd` tinyint(4) DEFAULT '0' COMMENT 'possibile duplicate (0=false, 1 true)',
  `us` tinyint(4) DEFAULT '0' COMMENT 'unislack (0=false, 1 true)',
  PRIMARY KEY (`chat_room_id`,`user_id`,`tag`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifying`
--

LOCK TABLES `notifying` WRITE;
/*!40000 ALTER TABLE `notifying` DISABLE KEYS */;
/*!40000 ALTER TABLE `notifying` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `questions`
--

DROP TABLE IF EXISTS `questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `questions` (
  `question_id` int(11) NOT NULL,
  `creation_date` int(11) NOT NULL,
  `owner_id` int(11) DEFAULT '0',
  `title` varchar(255) DEFAULT NULL,
  `tags` varchar(255) DEFAULT NULL,
  `score` int(11) DEFAULT '0',
  `view_count` int(11) DEFAULT NULL,
  `owner_rep` int(11) DEFAULT NULL,
  `answer_count` int(4) DEFAULT '0',
  `accepted_answer_id` int(11) DEFAULT '0',
  `comments_count` int(4) DEFAULT '0',
  `delete_vote_count` int(3) DEFAULT '0',
  `close_vote_count` int(2) DEFAULT '0',
  `closed_date` int(11) DEFAULT '0',
  `closed_reason` varchar(255) DEFAULT NULL,
  `duplicate_comment_creation_date` int(11) DEFAULT NULL,
  `dupicate_comment_user_id` int(11) DEFAULT '0',
  `duplicate_comment_body` varchar(254) DEFAULT NULL,
  `duplicate_comment_score` int(4) DEFAULT NULL,
  `duplicate_comment_target_score` int(11) DEFAULT NULL,
  `duplicate_response_body` varchar(45) DEFAULT NULL,
  `ignore_question_user_id` int(11) DEFAULT '0',
  `insertion_date` int(11) DEFAULT '0',
  `scan_date` int(11) DEFAULT '0',
  `notification_date` int(11) DEFAULT '0' COMMENT 'don''t notify to new room if already notified for example 2 min ago',
  `rooms_notified` varchar(255) DEFAULT '' COMMENT 'String of room id''s notified es 1223,1223,2321',
  PRIMARY KEY (`question_id`),
  KEY `tags` (`tags`),
  KEY `closeDate` (`closed_date`),
  KEY `duplicateComment` (`duplicate_comment_creation_date`),
  KEY `scanDate` (`scan_date`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `questions`
--

LOCK TABLES `questions` WRITE;
/*!40000 ALTER TABLE `questions` DISABLE KEYS */;
/*!40000 ALTER TABLE `questions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tags_tracked`
--

DROP TABLE IF EXISTS `tags_tracked`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tags_tracked` (
  `id_tag` int(11) NOT NULL AUTO_INCREMENT,
  `tag_name` varchar(45) NOT NULL,
  PRIMARY KEY (`id_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tags_tracked`
--

LOCK TABLES `tags_tracked` WRITE;
/*!40000 ALTER TABLE `tags_tracked` DISABLE KEYS */;
/*!40000 ALTER TABLE `tags_tracked` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `id_user` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `privilege_level` int(2) DEFAULT '0' COMMENT '0, usebot\n1, admin',
  PRIMARY KEY (`id_user`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2016-05-09 16:53:06
