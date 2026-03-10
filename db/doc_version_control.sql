-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: doc_version_control
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `approvals`
--

DROP TABLE IF EXISTS `approvals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approvals` (
  `approval_id` int NOT NULL AUTO_INCREMENT,
  `version_id` int NOT NULL,
  `reviewed_by` int NOT NULL,
  `decision` enum('approved','rejected') NOT NULL,
  `comment` text,
  `reviewed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`approval_id`),
  KEY `version_id` (`version_id`),
  KEY `reviewed_by` (`reviewed_by`),
  CONSTRAINT `approvals_ibfk_1` FOREIGN KEY (`version_id`) REFERENCES `document_versions` (`version_id`),
  CONSTRAINT `approvals_ibfk_2` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit_log`
--

DROP TABLE IF EXISTS `audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_log` (
  `log_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `action` varchar(100) NOT NULL,
  `document_id` int DEFAULT NULL,
  `version_id` int DEFAULT NULL,
  `file_id` int DEFAULT NULL,
  `target_user_id` int DEFAULT NULL,
  `details` text,
  `performed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `user_id` (`user_id`),
  KEY `document_id` (`document_id`),
  KEY `version_id` (`version_id`),
  KEY `file_id` (`file_id`),
  KEY `target_user_id` (`target_user_id`),
  CONSTRAINT `audit_log_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `audit_log_ibfk_2` FOREIGN KEY (`document_id`) REFERENCES `documents` (`document_id`),
  CONSTRAINT `audit_log_ibfk_3` FOREIGN KEY (`version_id`) REFERENCES `document_versions` (`version_id`),
  CONSTRAINT `audit_log_ibfk_4` FOREIGN KEY (`file_id`) REFERENCES `document_files` (`file_id`),
  CONSTRAINT `audit_log_ibfk_5` FOREIGN KEY (`target_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `document_files`
--

DROP TABLE IF EXISTS `document_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document_files` (
  `file_id` int NOT NULL AUTO_INCREMENT,
  `document_id` int NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_path` varchar(500) NOT NULL,
  `file_size_bytes` int DEFAULT NULL,
  `mime_type` varchar(100) DEFAULT NULL,
  `uploaded_by` int NOT NULL,
  `uploaded_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`file_id`),
  KEY `document_id` (`document_id`),
  KEY `uploaded_by` (`uploaded_by`),
  CONSTRAINT `document_files_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `documents` (`document_id`),
  CONSTRAINT `document_files_ibfk_2` FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `document_versions`
--

DROP TABLE IF EXISTS `document_versions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document_versions` (
  `version_id` int NOT NULL AUTO_INCREMENT,
  `document_id` int NOT NULL,
  `version_major` int NOT NULL DEFAULT '1',
  `version_minor` int NOT NULL DEFAULT '0',
  `version_number` varchar(20) NOT NULL,
  `content` text NOT NULL,
  `status` enum('draft','pending','approved','rejected') NOT NULL DEFAULT 'draft',
  `created_by` int NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `parent_version_id` int DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`version_id`),
  UNIQUE KEY `document_id` (`document_id`,`version_number`),
  KEY `created_by` (`created_by`),
  KEY `parent_version_id` (`parent_version_id`),
  CONSTRAINT `document_versions_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `documents` (`document_id`),
  CONSTRAINT `document_versions_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `document_versions_ibfk_3` FOREIGN KEY (`parent_version_id`) REFERENCES `document_versions` (`version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `documents`
--

DROP TABLE IF EXISTS `documents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `documents` (
  `document_id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `description` text,
  `created_by` int NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`document_id`),
  KEY `created_by` (`created_by`),
  CONSTRAINT `documents_ibfk_1` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `file_changes`
--

DROP TABLE IF EXISTS `file_changes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_changes` (
  `change_id` int NOT NULL AUTO_INCREMENT,
  `version_id` int NOT NULL,
  `file_id` int NOT NULL,
  `change_type` enum('added','modified','deleted','renamed') NOT NULL,
  `old_file_name` varchar(255) DEFAULT NULL,
  `change_summary` text,
  `changed_by` int NOT NULL,
  `changed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`change_id`),
  KEY `version_id` (`version_id`),
  KEY `file_id` (`file_id`),
  KEY `changed_by` (`changed_by`),
  CONSTRAINT `file_changes_ibfk_1` FOREIGN KEY (`version_id`) REFERENCES `document_versions` (`version_id`),
  CONSTRAINT `file_changes_ibfk_2` FOREIGN KEY (`file_id`) REFERENCES `document_files` (`file_id`),
  CONSTRAINT `file_changes_ibfk_3` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `role` enum('author','reviewer','reader','administrator') NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `created_by_admin` int DEFAULT NULL,
  `deleted_by_admin` int DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `created_by_admin` (`created_by_admin`),
  KEY `deleted_by_admin` (`deleted_by_admin`),
  CONSTRAINT `users_ibfk_1` FOREIGN KEY (`created_by_admin`) REFERENCES `users` (`user_id`),
  CONSTRAINT `users_ibfk_2` FOREIGN KEY (`deleted_by_admin`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-10 21:39:17
