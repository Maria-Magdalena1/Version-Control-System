# Version Control System - Документация

## Съдържание
- [Описание на проекта](#описание-на-проекта)
- [Технологии](#технологии)
- [Архитектура на системата](#архитектура-на-системата)
- [Диаграми](#диаграми)
- [База данни](#база-данни)
- [Подробна документация на функциите](#подробна-документация-на-функциите)
  - [UserService](#userservice)
  - [DocumentService](#documentservice)
  - [DocumentVersionService](#documentversionservice)
  - [ApprovalService](#approvalservice)
  - [DocumentFileService](#documentfileservice)
  - [AuditLogService](#auditlogservice)
  - [FileChangeService](#filechangeservice)
- [Конфигурация](#конфигурация)
- [Старт на приложението](#старт-на-приложението)

---

## Описание на проекта

Version Control System е Spring Boot приложение за управление на документи с версии, одобрения и аудит. Системата поддържа ролево базиран достъп (RBAC) и пълна история на промените.

### Основни функционалности:
- Управление на потребители с роли (READER, AUTHOR, REVIEWER, ADMINISTRATOR)
- Създаване и управление на документи
- Версиониране на документи (semantic versioning)
- Процес на одобрение на версии
- Качване и управление на файлове към документи
- Експорт на документи в PDF формат
- Пълен аудит лог на всички действия
- Сравняване на версии

---

## Технологии

- **Java 17+**
- **Spring Boot 3.x**
- **Spring Security** - автентикация и авторизация
- **Spring Data JPA** - ORM слой
- **MySQL** - релационна база данни
- **Hibernate** - JPA имплементация
- **iText PDF** - генериране на PDF документи
- **Lombok** - редуциране на boilerplate код
- **BCrypt** - криптиране на пароли

---

## Архитектура на системата

Проектът следва многослойна архитектура:

```
┌─────────────────┐
│   UI Layer      │ - ConsoleRunner
├─────────────────┤
│ Service Layer   │ - Business Logic
├─────────────────┤
│Repository Layer │ - Data Access
├─────────────────┤
│ Entity Layer    │ - Database Models
└─────────────────┘
```

### Пакетна структура:
- `main.entities` - JPA ентитита (модели на базата данни)
- `main.repositories` - Spring Data репозитории
- `main.services` - Логика
- `main.web` - DTOs и уеб слой
- `main.config` - Конфигурационни класове
- `main.exceptions` - Персонализирани изключения
- `main.ui` - Потребителски интерфейс

---

## База данни

### Структура на базата данни

Системата използва MySQL база данни с име `version_control_system`.

#### Основни таблици:

**1. users** - Потребители на системата
- `user_id` (UUID, PK) - уникален идентификатор
- `username` (VARCHAR(50), UNIQUE) - потребителско име
- `email` (VARCHAR(100), UNIQUE) - имейл адрес
- `full_name` (VARCHAR(100)) - пълно име
- `password` (VARCHAR) - хеширана парола (BCrypt)
- `role` (ENUM) - роля: READER, AUTHOR, REVIEWER, ADMINISTRATOR
- `is_active` (BOOLEAN) - активен/деактивиран акаунт
- `created_at` (TIMESTAMP) - дата на създаване
- `updated_at` (TIMESTAMP) - дата на последна промяна
- `updated_by` (UUID, FK) - кой е направил промяната

**2. documents** - Документи
- `document_id` (UUID, PK) - уникален идентификатор
- `title` (VARCHAR(200)) - заглавие на документа
- `description` (TEXT) - описание
- `created_by` (UUID, FK → users) - автор на документа
- `created_at` (TIMESTAMP) - дата на създаване
- `updated_at` (TIMESTAMP) - дата на последна промяна

**3. document_versions** - Версии на документи
- `version_id` (UUID, PK) - уникален идентификатор на версията
- `document_id` (UUID, FK → documents) - към кой документ принадлежи
- `version_major` (INT) - major версия
- `version_minor` (INT) - minor версия
- `version_patch` (INT) - patch версия
- `version_number` (VARCHAR(20)) - пълен номер на версията (напр. "1.2.3")
- `content` (TEXT) - съдържание на версията
- `status` (ENUM) - статус: DRAFT, PENDING, APPROVED, REJECTED
- `created_by` (UUID, FK → users) - създател на версията
- `created_at` (TIMESTAMP) - дата на създаване
- `updated_at` (TIMESTAMP) - дата на промяна
- `parent_version_id` (UUID, FK → document_versions) - родителска версия
- `comment` (TEXT) - коментар (при отхвърляне)
- `is_active` (BOOLEAN) - активна ли е версията

**4. approvals** - Одобрения на версии
- `approval_id` (UUID, PK) - уникален идентификатор
- `version_id` (UUID, FK → document_versions) - версия за одобрение
- `reviewed_by` (UUID, FK → users) - reviewer
- `decision` (ENUM) - APPROVED или REJECTED
- `comment` (TEXT) - коментар от reviewer
- `reviewed_at` (TIMESTAMP) - дата на одобрение/отхвърляне

**5. document_files** - Файлове прикачени към документи
- `file_id` (UUID, PK) - уникален идентификатор
- `document_id` (UUID, FK → documents) - към кой документ принадлежи
- `file_name` (VARCHAR) - име на файла
- `file_path` (VARCHAR) - път до файла на диска
- `file_size_bytes` (BIGINT) - размер на файла в байтове
- `mime_type` (VARCHAR(100)) - MIME тип
- `uploaded_by` (UUID, FK → users) - кой е качил файла
- `uploaded_at` (TIMESTAMP) - дата на качване
- `is_deleted` (BOOLEAN) - изтрит ли е (soft delete)

**6. file_changes** - История на промените на файлове
- `change_id` (UUID, PK) - уникален идентификатор
- `version_id` (UUID, FK → document_versions) - версия на документа
- `file_id` (UUID, FK → document_files) - файл
- `change_type` (ENUM) - ADDED, MODIFIED, DELETED
- `change_summary` (TEXT) - описание на промяната
- `changed_by` (UUID, FK → users) - кой е направил промяната
- `changed_at` (TIMESTAMP) - дата на промяната

**7. audit_log** - Аудит лог на всички действия
- `log_id` (UUID, PK) - уникален идентификатор
- `user_id` (UUID, FK → users) - потребител, извършил действието
- `action` (VARCHAR(100)) - тип на действието
- `document_id` (UUID, FK → documents) - засегнат документ (nullable)
- `version_id` (UUID, FK → document_versions) - засегната версия (nullable)
- `file_id` (UUID, FK → document_files) - засегнат файл (nullable)
- `target_user_id` (UUID, FK → users) - целеви потребител (nullable)
- `details` (TEXT) - подробности
- `performed_at` (TIMESTAMP) - дата и час на действието

### Връзки между таблиците:
- **One-to-Many**: User → Documents (потребител може да създаде много документи)
- **One-to-Many**: Document → DocumentVersions (документ има много версии)
- **One-to-Many**: DocumentVersion → Approvals (версия може да има много одобрения)
- **One-to-Many**: Document → DocumentFiles (документ може да има много файлове)
- **Many-to-One**: DocumentVersion → DocumentVersion (parent-child връзка за версии)

---

## Подробна документация на функциите

### UserService

**Локация:** `src/main/java/main/services/UserService.java`

Управлява потребителите и автентикацията.

#### Функции:

##### `register(UserDTO userDto)`
Регистрира нов потребител в системата.

**Параметри:**
- `userDto` - DTO обект съдържащ username, email, fullName, password, confirmPassword

**Валидации:**
- Username трябва да е между 3 и 20 символа
- Паролата трябва да е поне 6 символа
- Email трябва да съдържа '@'
- Username трябва да е уникален
- Email трябва да е уникален
- Паролите трябва да съвпадат

**Логика:**
- Първият регистриран потребител получава роля ADMINISTRATOR
- Всички останали получават роля READER по подразбиране
- Паролата се хешира с BCrypt
- Създава се audit log запис

**Изключения:**
- `IllegalArgumentException` - при невалидни входни данни
- `EntityAlreadyExistsException` - при съществуващо username/email
- `PasswordsDoNotMatchException` - при несъвпадащи пароли

---

##### `login(String username, String password)`
Влиза в системата.

**Параметри:**
- `username` - потребителско име
- `password` - парола

**Логика:**
- Проверява дали потребителят е активен
- Проверява паролата с BCrypt
- Създава Security Context с роля на потребителя
- Записва login в audit log

**Изключения:**
- `UserNotFoundException` - потребителят не съществува
- `DisabledException` - потребителят е деактивиран
- `BadCredentialsException` - грешна парола

---

##### `changeUserRole(UUID userId, Role newRole)`
Променя ролята на потребител (само за ADMINISTRATOR).

**Аннотации:** `@PreAuthorize("hasRole('ADMINISTRATOR')")`

**Параметри:**
- `userId` - ID на потребителя
- `newRole` - новата роля

**Логика:**
- Взема текущия администратор от Security Context
- Променя ролята на целевия потребител
- Записва промяната в audit log

**Локация в кода:** `UserService.java:132`

---

##### `changePassword(UUID userId, String oldPassword, String newPassword)`
Променя паролата на потребител.

**Параметри:**
- `userId` - ID на потребителя
- `oldPassword` - текуща парола
- `newPassword` - нова парола

**Валидации:**
- Потребителят трябва да е активен
- Старата парола трябва да е вярна

**Изключения:**
- `DisabledException` - потребителят е деактивиран
- `IncorrectPasswordException` - грешна текуща парола

**Локация в кода:** `UserService.java:152`

---

##### `deactivate(UUID userId, UUID adminId)`
Деактивира потребител (само за ADMINISTRATOR).

**Аннотации:** `@PreAuthorize("hasRole('ADMINISTRATOR')")`

**Параметри:**
- `userId` - ID на потребителя за деактивиране
- `adminId` - ID на администратора

**Изключения:**
- `IllegalStateException` - потребителят вече е деактивиран

**Локация в кода:** `UserService.java:172`

---

##### `activate(UUID userId, UUID adminId)`
Активира деактивиран потребител (само за ADMINISTRATOR).

**Аннотации:** `@PreAuthorize("hasRole('ADMINISTRATOR')")`

**Параметри:**
- `userId` - ID на потребителя
- `adminId` - ID на администратора

**Изключения:**
- `IllegalStateException` - потребителят вече е активен

**Локация в кода:** `UserService.java:191`

---

##### `loadUserByUsername(String username)`
Spring Security метод за зареждане на потребител.

**Параметри:**
- `username` - потребителско име

**Връща:** `UserDetails` обект за Spring Security

**Изключения:**
- `UsernameNotFoundException` - потребителят не съществува
- `DisabledException` - потребителят е деактивиран

**Локация в кода:** `UserService.java:214`

---

### DocumentService

**Локация:** `src/main/java/main/services/DocumentService.java`

Управлява документи и техните операции.

#### Функции:

##### `create(DocumentDTO dto, User author)`
Създава нов документ с първоначална версия 1.0.0.

**Аннотации:** `@PreAuthorize("hasRole('AUTHOR')")`

**Параметри:**
- `dto` - DTO с title, description, content, filePath
- `author` - потребител-автор

**Валидации:**
- Заглавието трябва да е поне 3 символа

**Логика:**
- Създава документ със статус DRAFT
- Автоматично създава версия 1.0.0
- Ако е предоставен filePath, качва файл
- Записва всичко в audit log

**Локация в кода:** `DocumentService.java:48`

---

##### `submitForReview(UUID versionId, User author)`
Подава версия за одобрение от reviewer.

**Аннотации:** `@PreAuthorize("hasRole('AUTHOR')")`

**Параметри:**
- `versionId` - ID на версията
- `author` - автор на документа

**Валидации:**
- Само авторът може да подаде за review
- Само DRAFT версии могат да се подават

**Логика:**
- Променя статуса на версията от DRAFT на PENDING
- Записва в audit log

**Изключения:**
- `UnauthorizedException` - потребителят не е автор
- `InvalidVersionStatusException` - версията не е DRAFT

**Локация в кода:** `DocumentService.java:75`

---

##### `createNewVersion(UUID documentId, String changeType, String content, String filePath)`
Създава нова версия на съществуващ документ.

**Аннотации:** `@PreAuthorize("hasRole('AUTHOR')")`

**Параметри:**
- `documentId` - ID на документа
- `changeType` - "major", "minor" или "patch"
- `content` - ново съдържание
- `filePath` - път до файл (optional)

**Логика:**
- Взема активната версия
- Инкрементва версията според changeType:
  - "major": 1.2.3 → 2.0.0
  - "minor": 1.2.3 → 1.3.0
  - "patch": 1.2.3 → 1.2.4
- Създава нова версия със статус DRAFT
- Качва файл, ако е предоставен

**Изключения:**
- `DocumentNotFoundException` - документът не съществува
- `ActiveVersionNotFoundException` - няма активна версия

**Локация в кода:** `DocumentService.java:94`

---

##### `getVersionHistory(UUID documentId)`
Връща историята на версиите на документ.

**Аннотации:** `@PreAuthorize("hasAnyRole('AUTHOR', 'REVIEWER', 'ADMINISTRATOR')")`

**Параметри:**
- `documentId` - ID на документа

**Връща:** `List<DocumentVersion>` - всички версии, сортирани по дата

**Локация в кода:** `DocumentService.java:103`

---

##### `compareVersions(UUID versionId1, UUID versionId2)`
Сравнява две версии на документ.

**Аннотации:** `@PreAuthorize("hasAnyRole('AUTHOR', 'REVIEWER', 'READER', 'ADMINISTRATOR')")`

**Параметри:**
- `versionId1` - ID на първата версия
- `versionId2` - ID на втората версия

**Връща:** `VersionComparisonResult` обект съдържащ:
- Метаданни на двете версии
- Добавени редове
- Премахнати редове

**Локация в кода:** `DocumentService.java:111`

---

##### `getActiveVersion(UUID documentId)`
Връща активната версия на документ.

**Аннотации:** `@PreAuthorize("hasAnyRole('READER', 'AUTHOR', 'REVIEWER', 'ADMINISTRATOR')")`

**Параметри:**
- `documentId` - ID на документа

**Връща:** `DocumentVersion` - активната версия

**Изключения:**
- `ActiveVersionNotFoundException` - няма активна версия

**Локация в кода:** `DocumentService.java:116`

---

##### `getApprovedVersions(UUID documentId)`
Връща всички одобрени версии на документ.

**Аннотации:** `@PreAuthorize("hasAnyRole('READER', 'AUTHOR', 'REVIEWER', 'ADMINISTRATOR')")`

**Параметри:**
- `documentId` - ID на документа

**Връща:** `List<DocumentVersion>` - всички APPROVED версии

**Локация в кода:** `DocumentService.java:123`

---

##### `exportToPdf(UUID documentId)`
Експортира активната версия на документ в PDF формат.

**Аннотации:** `@PreAuthorize("hasAnyRole('READER', 'AUTHOR', 'REVIEWER', 'ADMINISTRATOR')")`

**Параметри:**
- `documentId` - ID на документа

**Връща:** `byte[]` - PDF файл като байтов масив

**PDF съдържа:**
- Заглавие на документа
- Номер на версията
- Автор
- Дата на създаване
- Статус
- Съдържание

**Логика:**
- Използва iText библиотека
- Записва експорт действието в audit log

**Изключения:**
- `RuntimeException` - грешка при генериране на PDF

**Локация в кода:** `DocumentService.java:131`

---

### DocumentVersionService

**Локация:** `src/main/java/main/services/DocumentVersionService.java`

Управлява версиите на документите.

#### Функции:

##### `createDocumentVersion(Document document, DocumentVersion lastVersion, int versionMajor, int versionMinor, int versionPatch, String versionNumber, String content)`
Създава нова версия на документ.

**Параметри:**
- `document` - документът
- `lastVersion` - родителска версия (може да е null за първа версия)
- `versionMajor` - major номер
- `versionMinor` - minor номер
- `versionPatch` - patch номер
- `versionNumber` - пълен номер (напр. "1.2.3")
- `content` - съдържание

**Логика:**
- Създава версия със статус DRAFT
- Версията е неактивна по подразбиране
- Записва автора от документа
- Свързва с parent версията

**Връща:** Създадената `DocumentVersion`

**Локация в кода:** `DocumentVersionService.java:39`

---

##### `createNewVersion(Document document, String changeType, String content, String filePath)`
Създава нова версия базирана на активната.

**Аннотации:** `@PreAuthorize("hasRole('AUTHOR')")`

**Параметри:**
- `document` - документът
- `changeType` - "major", "minor" или "patch"
- `content` - ново съдържание
- `filePath` - път до файл (optional)

**Логика:**
- Намира активната версия
- Изчислява новия номер на версията според типа промяна
- Създава нова версия
- Качва файл ако е предоставен

**Изключения:**
- `ActiveVersionNotFoundException` - няма активна версия

**Локация в кода:** `DocumentVersionService.java:62`

---

##### `activateVersion(DocumentVersion version, User reviewer)`
Активира версия и деактивира текущата активна.

**Параметри:**
- `version` - версията за активиране
- `reviewer` - потребител, който активира

**Логика:**
- Намира текущата активна версия и я деактивира
- Активира новата версия
- Записва в audit log

**Локация в кода:** `DocumentVersionService.java:98`

---

##### `rollbackToPreviousVersion(DocumentVersion version, User reviewer)`
Връща към предишна версия при отхвърляне.

**Параметри:**
- `version` - отхвърлената версия
- `reviewer` - reviewer

**Логика:**
- Деактивира текущата версия
- Ако има parent версия, активира я
- Ако няма parent (първа версия), връща я към DRAFT статус
- Записва в audit log

**Локация в кода:** `DocumentVersionService.java:111`

---

##### `compareVersions(UUID version1, UUID version2)`
Сравнява съдържанието на две версии.

**Параметри:**
- `version1` - ID на първата версия
- `version2` - ID на втората версия

**Връща:** `VersionComparisonResult` с:
- Метаданни (версия, автор, дата, статус) за двете версии
- Добавени редове (в version2, но не в version1)
- Премахнати редове (в version1, но не в version2)

**Алгоритъм:**
- Разделя съдържанието на редове
- Филтрира редовете за намиране на разлики

**Локация в кода:** `DocumentVersionService.java:128`

---

##### `findDocumentVersionByDocumentIdAndIsActiveIsTrue(UUID documentId)`
Намира активната версия на документ.

**Параметри:**
- `documentId` - ID на документа

**Връща:** `Optional<DocumentVersion>`

**Локация в кода:** `DocumentVersionService.java:158`

---

##### `getVersionHistory(UUID documentId)`
Връща всички версии на документ, сортирани по дата.

**Параметри:**
- `documentId` - ID на документа

**Връща:** `List<DocumentVersion>`

**Локация в кода:** `DocumentVersionService.java:167`

---

##### `findApprovedVersionsByDocumentId(UUID documentId)`
Връща всички одобрени версии на документ.

**Параметри:**
- `documentId` - ID на документа

**Връща:** `List<DocumentVersion>` - версии със статус APPROVED

**Локация в кода:** `DocumentVersionService.java:171`

---

### ApprovalService

**Локация:** `src/main/java/main/services/ApprovalService.java`

Управлява процеса на одобрение на версии.

#### Функции:

##### `reviewVersion(UUID versionId, boolean isApproved, String comment, User reviewer)`
Одобрява или отхвърля версия.

**Аннотации:** `@PreAuthorize("hasRole('REVIEWER')")`

**Параметри:**
- `versionId` - ID на версията
- `isApproved` - true за одобрение, false за отхвърляне
- `comment` - коментар от reviewer
- `reviewer` - потребител reviewer

**Валидации:**
- Само PENDING версии могат да се одобряват

**Логика:**
- Създава Approval запис
- При одобрение: активира версията
- При отхвърляне: връща към предишна версия
- Записва всичко в audit log

**Изключения:**
- `InvalidVersionStatusException` - версията не е PENDING

**Локация в кода:** `ApprovalService.java:33`

---

##### `approveVersion(DocumentVersion version, User reviewer)`
Одобрява версия.

**Аннотации:** `@PreAuthorize("hasRole('REVIEWER')")`

**Параметри:**
- `version` - версията
- `reviewer` - reviewer

**Логика:**
- Променя статуса на APPROVED
- Активира версията
- Обновява updatedAt на документа
- Записва в audit log

**Локация в кода:** `ApprovalService.java:60`

---

##### `rejectVersion(DocumentVersion version, User reviewer, String reason)`
Отхвърля версия.

**Аннотации:** `@PreAuthorize("hasRole('REVIEWER')")`

**Параметри:**
- `version` - версията
- `reviewer` - reviewer
- `reason` - причина за отхвърляне

**Логика:**
- Променя статуса на REJECTED
- Записва причината в коментар
- Rollback към предишна версия
- Обновява updatedAt на документа
- Записва в audit log

**Локация в кода:** `ApprovalService.java:72`

---

### DocumentFileService

**Локация:** `src/main/java/main/services/DocumentFileService.java`

Управлява файлове, прикачени към документи.

#### Функции:

##### `uploadFile(String sourcePath, Document document, DocumentVersion version, User uploadedBy)`
Качва файл към документ.

**Параметри:**
- `sourcePath` - път до файла на локалната система
- `document` - документът
- `version` - версията на документа
- `uploadedBy` - потребител, който качва

**Валидации:**
- Файлът трябва да съществува

**Логика:**
- Копира файла в директория `uploads/{documentId}/{filename}`
- Създава директории ако не съществуват
- Записва метаданни (име, размер, MIME тип)
- Създава FileChange запис с тип ADDED
- Записва в audit log

**Връща:** Създадения `DocumentFile`

**Изключения:**
- `IllegalArgumentException` - файлът не съществува
- `RuntimeException` - грешка при копиране

**Локация в кода:** `DocumentFileService.java:33`

---

##### `deleteFile(UUID fileId, DocumentVersion version, User deletedBy)`
Изтрива файл (soft delete).

**Параметри:**
- `fileId` - ID на файла
- `version` - версията на документа
- `deletedBy` - потребител, който изтрива

**Логика:**
- Маркира файла като изтрит (isDeleted = true)
- Създава FileChange запис с тип DELETED
- Записва в audit log

**Изключения:**
- `IllegalArgumentException` - файлът не е намерен

**Локация в кода:** `DocumentFileService.java:72`

---

##### `getFilesByDocument(UUID documentId)`
Връща всички файлове на документ.

**Параметри:**
- `documentId` - ID на документа

**Връща:** `List<DocumentFile>` - всички неизтрити файлове

**Локация в кода:** `DocumentFileService.java:89`

---

### AuditLogService

**Локация:** `src/main/java/main/services/AuditLogService.java`

Създава записи в audit log за всички действия в системата.

#### Функции:

##### `createLogForUser(User user, String action, User targetUser, String details)`
Създава лог за действие свързано с потребител.

**Параметри:**
- `user` - потребител, извършил действието
- `action` - тип на действието (напр. "USER REGISTERED", "CHANGE ROLE")
- `targetUser` - целеви потребител
- `details` - подробности

**Локация в кода:** `AuditLogService.java:22`

---

##### `createLogForDocument(User user, String action, Document document, DocumentVersion version, DocumentFile file, String details)`
Създава лог за действие свързано с документ.

**Параметри:**
- `user` - потребител, извършил действието
- `action` - тип на действието
- `document` - документът
- `version` - версията (nullable)
- `file` - файлът (nullable)
- `details` - подробности

**Примери за действия:**
- "CREATE DOCUMENT"
- "SUBMIT FOR REVIEW"
- "APPROVE VERSION"
- "REJECT VERSION"
- "EXPORT TO PDF"

**Локация в кода:** `AuditLogService.java:33`

---

##### `createLogForDocumentFile(User user, String action, Document document, DocumentVersion documentVersion, DocumentFile file, String details)`
Създава лог за действие с файл.

**Параметри:**
- `user` - потребител
- `action` - действие
- `document` - документ
- `documentVersion` - версия
- `file` - файл
- `details` - подробности

**Примери за действия:**
- "UPLOAD DOCUMENT FILE"
- "DELETE DOCUMENT FILE"
- "CHANGE DOCUMENT FILE"

**Локация в кода:** `AuditLogService.java:47`

---

##### `createLogForDocumentVersion(User reviewer, String action, Document document, DocumentVersion documentVersion, String details)`
Създава лог за действие с версия.

**Параметри:**
- `reviewer` - потребител
- `action` - действие
- `document` - документ
- `documentVersion` - версия
- `details` - подробности

**Примери за действия:**
- "ACTIVATE VERSION"
- "ROLLBACK TO DRAFT"

**Локация в кода:** `AuditLogService.java:60`

---

### FileChangeService

**Локация:** `src/main/java/main/services/FileChangeService.java`

Управлява промени на файлове между версии.

#### Функции:

##### `saveFile(FileChange fileChange)`
Записва промяна на файл.

**Параметри:**
- `fileChange` - обект FileChange

**Локация в кода:** `FileChangeService.java:19`

---

## Конфигурация

### application.properties

```properties
spring.application.name=version-control-system

# MySQL Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/version_control_system?createDatabaseIfNotExist=true
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### Необходими стъпки за конфигурация:

1. Инсталирайте MySQL Server
2. Заменете `YOUR_USERNAME` и `YOUR_PASSWORD` с вашите MySQL credentials
3. Базата данни ще се създаде автоматично при първо стартиране

---

## Старт на приложението

### Предварителни изисквания:
- Java 17 или по-нова версия
- Maven 3.6+
- MySQL Server 8.0+

Приложението е конзолно. Стартирайте го като изпълните `VersionControlSystemApplication.java`, след което `ConsoleRunner` ще поеме управлението на потребителския интерфейс.

---

## Роли и права на достъп

### READER
- Преглед на документи
- Преглед на одобрени версии
- Сравняване на версии
- Експорт на документи в PDF

### AUTHOR
- Всички права на READER
- Създаване на документи
- Създаване на нови версии
- Подаване за одобрение
- Качване на файлове

### REVIEWER
- Всички права на READER
- Одобряване на версии
- Отхвърляне на версии
- Преглед на версии в очакване

### ADMINISTRATOR
- Всички права на AUTHOR и REVIEWER
- Промяна на роли на потребители
- Активиране/Деактивиране на потребители
- Пълен достъп до audit log

---

## Workflow на одобрение

1. **AUTHOR** създава документ (статус: DRAFT, версия 1.0.0)
2. **AUTHOR** подава за одобрение (статус: PENDING)
3. **REVIEWER** преглежда и взима решение:
   - **APPROVED**: Версията става активна (статус: APPROVED)
   - **REJECTED**: Връща се към предишна версия или DRAFT
4. **AUTHOR** може да създаде нова версия базирана на одобрената

---

## Semantic Versioning

Системата използва semantic versioning (MAJOR.MINOR.PATCH):

- **MAJOR** (1.0.0 → 2.0.0): Съществени промени, несъвместими с предишни версии
- **MINOR** (1.0.0 → 1.1.0): Нова функционалност, съвместима с предишни версии
- **PATCH** (1.0.0 → 1.0.1): Малки корекции и bug fixes

---

## Audit Trail

Всички действия в системата се записват в audit log:
- Потребителски действия (регистрация, login, промяна на роля)
- Документни действия (създаване, редакция, одобрение)
- Файлови операции (качване, изтриване)
- Промени на версии (създаване, активиране, rollback)

Всеки лог съдържа:
- Кой е извършил действието
- Какво действие е извършено
- Кога е извършено
- Подробности за действието
