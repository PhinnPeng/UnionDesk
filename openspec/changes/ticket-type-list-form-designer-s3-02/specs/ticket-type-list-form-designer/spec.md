# ticket-type-list-form-designer

## ADDED Requirements

### Requirement: Ticket type list displays as card grid

The platform domain detail tickets sub-tab SHALL be labeled「工单列表」and display ticket types as a responsive card grid with centered content, showing icon, name, code, description excerpt, status, field count, state count, and an unpublished indicator when draft schema differs from published schema.

#### Scenario: User views ticket type list

- **WHEN** user opens domain detail tickets tab with existing ticket types
- **THEN** types are shown as cards not table rows
- **AND** the sub-tab label reads「工单列表」

#### Scenario: Unpublished draft indicator

- **WHEN** `form_schema_draft` differs from `form_schema` for a ticket type
- **THEN** the card shows an「未发布」tag

### Requirement: Ticket type code and name are unique per domain

The system SHALL enforce unique `code` and `name` per `business_domain_id` at database and service layer with user-facing Chinese error messages.

#### Scenario: Duplicate code on create

- **WHEN** user creates a ticket type with a code already used in the same domain
- **THEN** the API returns an error indicating the code already exists

### Requirement: Ticket type supports description and icon

Ticket types SHALL support optional `description` (varchar) and `icon` (Iconify identifier string) persisted and returned in admin APIs.

#### Scenario: Create with description and icon

- **WHEN** user creates a ticket type with description and icon
- **THEN** the created type includes those fields in GET list response

### Requirement: Form schema draft and publish separation

The system SHALL store `form_schema_draft` for unpublished edits and `form_schema` as the published version consumed by end users.

#### Scenario: Save draft from designer

- **WHEN** user clicks Save in the form designer
- **THEN** the current schema is persisted to `form_schema_draft` only

#### Scenario: Publish from designer

- **WHEN** user clicks Publish in the form designer
- **THEN** validated draft schema is copied to `form_schema`
- **AND** end-user facing APIs read only `form_schema`

#### Scenario: Publish when no draft exists

- **WHEN** user clicks Publish and `form_schema_draft` is null or identical to published schema
- **THEN** the publish operation succeeds idempotently using the current published schema as source

#### Scenario: Designer keyboard shortcut saves draft

- **WHEN** user presses Ctrl+S in the form designer
- **THEN** the current schema is persisted to `form_schema_draft` via the same API as the Save button

### Requirement: Legacy ticket type config route redirects to form design

Legacy routes `/platform/domains/ticket-type-config/{domainId}/{typeId}` and `.../form` SHALL redirect to `/platform/domains/ticket/form-design/{domainId}/{typeId}` for backward-compatible bookmarks.

#### Scenario: User opens legacy config URL

- **WHEN** user navigates to `/platform/domains/ticket-type-config/{domainId}/{typeId}` or `.../form`
- **THEN** the application redirects to `/platform/domains/ticket/form-design/{domainId}/{typeId}`

### Requirement: Create flow prompts optional form design entry

After creating a ticket type with code, name, description, and icon, the UI SHALL ask whether to open the dedicated form design page in a new tab.

#### Scenario: User confirms form design after create

- **WHEN** user confirms entering form design after create
- **THEN** a tab opens to `/platform/domains/ticket/form-design/{domainId}/{typeId}`

#### Scenario: User declines form design after create

- **WHEN** user declines
- **THEN** user remains on the ticket list with the new card visible

### Requirement: Card actions separate form design and metadata edit

Each ticket type card SHALL provide「表单设计」opening the dedicated form design page and「编辑」opening a modal to edit name, description, icon, and status with code read-only.

#### Scenario: Open form design from card

- **WHEN** user clicks「表单设计」on a card
- **THEN** the form design page tab opens with designer save/publish wired to APIs

#### Scenario: Edit metadata from card

- **WHEN** user clicks「编辑」on a card
- **THEN** a modal allows editing name, description, icon, status
- **AND** code field is read-only
