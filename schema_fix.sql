DROP TABLE IF EXISTS `workflow_sso_providers`;
CREATE TABLE `workflow_sso_providers` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `site_id` bigint unsigned NOT NULL DEFAULT '1',
  `name` varchar(255) NOT NULL,
  `alias` varchar(255) NOT NULL,
  `description` text,
  `driver` varchar(255) NOT NULL DEFAULT 'opaque',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `config` text,
  `on_failure` varchar(255) NOT NULL DEFAULT 'reject',
  `cache_ttl` int NOT NULL DEFAULT '300',
  `publish_status` tinyint(1) NOT NULL DEFAULT '1',
  `insert_by` bigint unsigned DEFAULT NULL,
  `update_by` bigint unsigned DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  `deleted_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `workflow_sso_providers_site_alias_unique` (`site_id`,`alias`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
