CREATE TABLE app_users (
  id UUID PRIMARY KEY,
  email VARCHAR(320) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  token_version INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE user_roles (
  user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
  role VARCHAR(32) NOT NULL CHECK (role IN ('CUSTOMER','ORGANIZER','STAFF','ADMIN')),
  PRIMARY KEY (user_id, role)
);
CREATE TABLE events (
  id UUID PRIMARY KEY,
  organizer_id UUID NOT NULL REFERENCES app_users(id),
  title VARCHAR(200) NOT NULL,
  description TEXT,
  venue VARCHAR(250) NOT NULL,
  starts_at TIMESTAMPTZ NOT NULL,
  ends_at TIMESTAMPTZ NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PUBLISHED','CANCELLED','COMPLETED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (ends_at > starts_at)
);
CREATE INDEX idx_events_status_starts ON events(status, starts_at);
CREATE INDEX idx_events_organizer ON events(organizer_id);
CREATE TABLE event_staff (
  event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES app_users(id),
  PRIMARY KEY (event_id, user_id)
);
CREATE TABLE ticket_types (
  id UUID PRIMARY KEY,
  event_id UUID NOT NULL REFERENCES events(id),
  name VARCHAR(100) NOT NULL,
  price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
  capacity INTEGER NOT NULL CHECK (capacity > 0),
  available_quantity INTEGER NOT NULL CHECK (available_quantity >= 0 AND available_quantity <= capacity),
  sale_starts_at TIMESTAMPTZ NOT NULL,
  sale_ends_at TIMESTAMPTZ NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  UNIQUE (event_id, name),
  CHECK (sale_ends_at > sale_starts_at)
);
CREATE INDEX idx_ticket_types_event ON ticket_types(event_id);
CREATE TABLE promotions (
  id UUID PRIMARY KEY,
  event_id UUID NOT NULL REFERENCES events(id),
  code VARCHAR(64) NOT NULL UNIQUE,
  discount_type VARCHAR(32) NOT NULL CHECK (discount_type IN ('PERCENTAGE','FIXED')),
  discount_value NUMERIC(12,2) NOT NULL CHECK (discount_value > 0),
  starts_at TIMESTAMPTZ NOT NULL,
  ends_at TIMESTAMPTZ NOT NULL,
  max_uses INTEGER NOT NULL CHECK (max_uses > 0),
  used_count INTEGER NOT NULL DEFAULT 0 CHECK (used_count >= 0 AND used_count <= max_uses),
  CHECK (ends_at > starts_at),
  CHECK (discount_type <> 'PERCENTAGE' OR discount_value <= 100)
);
CREATE TABLE reservations (
  id UUID PRIMARY KEY,
  customer_id UUID NOT NULL REFERENCES app_users(id),
  ticket_type_id UUID NOT NULL REFERENCES ticket_types(id),
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING','CONFIRMED','EXPIRED','CANCELLED')),
  expires_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_reservations_expiry ON reservations(expires_at) WHERE status = 'PENDING';
CREATE TABLE orders (
  id UUID PRIMARY KEY,
  customer_id UUID NOT NULL REFERENCES app_users(id),
  reservation_id UUID NOT NULL UNIQUE REFERENCES reservations(id),
  promotion_id UUID REFERENCES promotions(id),
  subtotal NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0),
  discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
  total_amount NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
  status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING_PAYMENT','PAID','FAILED','CANCELLED','EXPIRED','REFUNDED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (total_amount = subtotal - discount_amount)
);
CREATE INDEX idx_orders_customer ON orders(customer_id, created_at DESC);
CREATE TABLE order_items (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL REFERENCES orders(id),
  ticket_type_id UUID NOT NULL REFERENCES ticket_types(id),
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
  UNIQUE (order_id, ticket_type_id)
);
CREATE TABLE payments (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL REFERENCES orders(id),
  idempotency_key VARCHAR(128) NOT NULL UNIQUE,
  request_hash VARCHAR(64) NOT NULL,
  provider_reference VARCHAR(128),
  amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
  status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING','SUCCEEDED','FAILED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (order_id, idempotency_key)
);
CREATE TABLE tickets (
  id UUID PRIMARY KEY,
  order_item_id UUID NOT NULL REFERENCES order_items(id),
  owner_id UUID NOT NULL REFERENCES app_users(id),
  qr_token_hash VARCHAR(64) NOT NULL UNIQUE,
  status VARCHAR(32) NOT NULL CHECK (status IN ('VALID','USED','CANCELLED','REFUNDED')),
  issued_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_tickets_owner ON tickets(owner_id);
CREATE TABLE check_ins (
  id UUID PRIMARY KEY,
  ticket_id UUID NOT NULL UNIQUE REFERENCES tickets(id),
  event_id UUID NOT NULL REFERENCES events(id),
  staff_id UUID NOT NULL REFERENCES app_users(id),
  checked_in_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_check_ins_event ON check_ins(event_id, checked_in_at);
