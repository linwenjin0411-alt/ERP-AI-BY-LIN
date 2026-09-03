create table if not exists erp_companies (
  id bigint primary key auto_increment,
  code varchar(40) not null unique,
  name varchar(160) not null,
  active tinyint(1) not null default 1,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp on update current_timestamp
) engine=InnoDB default charset=utf8mb4;

create table if not exists erp_roles (
  id bigint primary key auto_increment,
  code varchar(40) not null unique,
  name varchar(160) not null,
  active tinyint(1) not null default 1,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp on update current_timestamp
) engine=InnoDB default charset=utf8mb4;

create table if not exists erp_users (
  id bigint primary key auto_increment,
  username varchar(80) not null unique,
  password_hash char(64) not null,
  display_name varchar(160) not null,
  email varchar(160),
  company_id bigint not null,
  role_id bigint not null,
  active tinyint(1) not null default 1,
  last_login_at timestamp null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp on update current_timestamp,
  constraint fk_erp_users_company foreign key (company_id) references erp_companies(id),
  constraint fk_erp_users_role foreign key (role_id) references erp_roles(id)
) engine=InnoDB default charset=utf8mb4;

create table if not exists erp_menus (
  id bigint primary key auto_increment,
  code varchar(80) not null unique,
  parent_code varchar(80),
  name_key varchar(120) not null,
  module_code varchar(80) not null,
  sort_order int not null default 0,
  active tinyint(1) not null default 1,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp on update current_timestamp
) engine=InnoDB default charset=utf8mb4;

create table if not exists erp_role_menus (
  role_id bigint not null,
  menu_id bigint not null,
  can_view tinyint(1) not null default 1,
  can_create tinyint(1) not null default 0,
  can_update tinyint(1) not null default 0,
  can_approve tinyint(1) not null default 0,
  primary key (role_id, menu_id),
  constraint fk_erp_role_menus_role foreign key (role_id) references erp_roles(id),
  constraint fk_erp_role_menus_menu foreign key (menu_id) references erp_menus(id)
) engine=InnoDB default charset=utf8mb4;

create table if not exists erp_modules (
  code varchar(80) primary key,
  title_key varchar(120) not null,
  subtitle_key varchar(120) not null,
  page_type varchar(30) not null,
  table_title_key varchar(120) not null,
  process_title_key varchar(120) not null,
  focus_title_key varchar(120) not null,
  prompt_value varchar(500),
  sort_order int not null default 0,
  active tinyint(1) not null default 1,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp on update current_timestamp
) engine=InnoDB default charset=utf8mb4;

create table if not exists erp_module_actions (
  module_code varchar(80) not null,
  sort_order int not null,
  action_key varchar(120) not null,
  primary key (module_code, sort_order),
  constraint fk_erp_module_actions_module foreign key (module_code) references erp_modules(code)
) engine=InnoDB default charset=utf8mb4;

create table if not exists erp_module_process_steps (
  module_code varchar(80) not null,
  sort_order int not null,
  label_value varchar(255) not null,
  primary key (module_code, sort_order),
  constraint fk_erp_module_process_steps_module foreign key (module_code) references erp_modules(code)
) engine=InnoDB default charset=utf8mb4;

create table if not exists erp_module_metrics (
  module_code varchar(80) not null,
  location varchar(30) not null,
  sort_order int not null,
  label_value varchar(255) not null,
  metric_value varchar(80) not null,
  note_value varchar(255),
  accent_code varchar(30) not null default 'accent',
  primary key (module_code, location, sort_order),
  constraint fk_erp_module_metrics_module foreign key (module_code) references erp_modules(code)
) engine=InnoDB default charset=utf8mb4;

create table if not exists erp_module_table_columns (
  module_code varchar(80) not null,
  sort_order int not null,
  column_key varchar(120) not null,
  primary key (module_code, sort_order),
  constraint fk_erp_module_table_columns_module foreign key (module_code) references erp_modules(code)
) engine=InnoDB default charset=utf8mb4;

create table if not exists erp_module_table_rows (
  module_code varchar(80) not null,
  sort_order int not null,
  c1 varchar(255),
  c2 varchar(255),
  c3 varchar(255),
  c4 varchar(255),
  c5 varchar(255),
  c6 varchar(255),
  c7 varchar(255),
  c8 varchar(255),
  primary key (module_code, sort_order),
  constraint fk_erp_module_table_rows_module foreign key (module_code) references erp_modules(code)
) engine=InnoDB default charset=utf8mb4;

create table if not exists erp_module_focus_items (
  module_code varchar(80) not null,
  sort_order int not null,
  item_value varchar(500) not null,
  primary key (module_code, sort_order),
  constraint fk_erp_module_focus_items_module foreign key (module_code) references erp_modules(code)
) engine=InnoDB default charset=utf8mb4;

create table if not exists erp_item_masters (
  id bigint primary key auto_increment,
  item_code varchar(80) not null unique,
  item_name varchar(180) not null,
  item_type varchar(80) not null,
  uom varchar(30) not null,
  plant varchar(40) not null,
  status varchar(80) not null,
  safety_stock varchar(40),
  lead_time_days varchar(40),
  active tinyint(1) not null default 1,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp on update current_timestamp
) engine=InnoDB default charset=utf8mb4;

create table if not exists erp_function_records (
  id bigint primary key auto_increment,
  function_code varchar(80) not null,
  c1 varchar(255),
  c2 varchar(255),
  c3 varchar(255),
  c4 varchar(255),
  c5 varchar(255),
  c6 varchar(255),
  c7 varchar(255),
  c8 varchar(255),
  active tinyint(1) not null default 1,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp on update current_timestamp,
  index idx_erp_function_records_code (function_code)
) engine=InnoDB default charset=utf8mb4;

create table if not exists erp_licenses (
  id bigint primary key auto_increment,
  license_key varchar(500) not null,
  valid_from date not null,
  valid_until date not null,
  active tinyint(1) not null default 1,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp on update current_timestamp
) engine=InnoDB default charset=utf8mb4;

insert into erp_companies (code, name, active) values
('LINOVA', 'Linova Smart Manufacturing Ltd.', 1)
on duplicate key update name = values(name), active = values(active);

insert into erp_roles (code, name, active) values
('ADMIN', 'System Administrator', 1),
('PLANNER', 'Production Planner', 1)
on duplicate key update name = values(name), active = values(active);

insert into erp_modules (code, title_key, subtitle_key, page_type, table_title_key, process_title_key, focus_title_key, prompt_value, sort_order, active) values
('DASHBOARD', 'module.dashboard', 'dashboard.subtitle', 'DASHBOARD', 'table.sample', 'dashboard.process.title', 'dashboard.alerts.title', null, 10, 1),
('MASTER', 'module.master', 'master.subtitle', 'OPERATIONAL', 'table.worklist', 'panel.process', 'panel.todo', null, 20, 1),
('PROCUREMENT', 'module.procurement', 'procurement.subtitle', 'OPERATIONAL', 'table.worklist', 'panel.process', 'panel.todo', null, 30, 1),
('SALES', 'module.sales', 'sales.subtitle', 'OPERATIONAL', 'table.worklist', 'panel.process', 'panel.todo', null, 40, 1),
('INVENTORY', 'module.inventory', 'inventory.subtitle', 'OPERATIONAL', 'table.worklist', 'panel.process', 'panel.todo', null, 50, 1),
('MANUFACTURING', 'module.manufacturing', 'manufacturing.subtitle', 'OPERATIONAL', 'table.worklist', 'panel.process', 'panel.todo', null, 60, 1),
('FINANCE', 'module.finance', 'finance.subtitle', 'OPERATIONAL', 'table.worklist', 'panel.process', 'panel.todo', null, 70, 1),
('AI', 'module.ai', 'ai.subtitle', 'AI', 'module.ai', 'panel.process', 'section.analytics', 'ai.placeholder', 80, 1),
('ADMIN', 'module.admin', 'admin.subtitle', 'OPERATIONAL', 'table.worklist', 'panel.process', 'panel.todo', null, 90, 1)
on duplicate key update title_key = values(title_key), subtitle_key = values(subtitle_key), page_type = values(page_type),
table_title_key = values(table_title_key), process_title_key = values(process_title_key), focus_title_key = values(focus_title_key),
prompt_value = values(prompt_value), sort_order = values(sort_order), active = values(active);

insert into erp_menus (code, parent_code, name_key, module_code, sort_order, active) values
('COCKPIT', null, 'module.dashboard', 'DASHBOARD', 10, 1),
('MASTER', null, 'module.master', 'MASTER', 20, 1),
('PROCUREMENT', null, 'module.procurement', 'PROCUREMENT', 30, 1),
('SALES', null, 'module.sales', 'SALES', 40, 1),
('INVENTORY', null, 'module.inventory', 'INVENTORY', 50, 1),
('MANUFACTURING', null, 'module.manufacturing', 'MANUFACTURING', 60, 1),
('FINANCE', null, 'module.finance', 'FINANCE', 70, 1),
('AI', null, 'module.ai', 'AI', 80, 1),
('ADMIN', null, 'module.admin', 'ADMIN', 90, 1)
on duplicate key update name_key = values(name_key), module_code = values(module_code), sort_order = values(sort_order), active = values(active);

insert into erp_menus (code, parent_code, name_key, module_code, sort_order, active) values
('MASTER_MAINT', 'MASTER', 'menu.area.master.maintenance', 'MASTER', 10, 1),
('MASTER_GOVERNANCE', 'MASTER', 'menu.area.governance', 'MASTER', 20, 1),
('MASTER_PRODUCT', 'MASTER_MAINT', 'menu.section.product', 'MASTER', 10, 1),
('MASTER_PARTNER', 'MASTER_MAINT', 'menu.section.partner', 'MASTER', 20, 1),
('MASTER_LOGISTICS', 'MASTER_MAINT', 'menu.section.logistics', 'MASTER', 30, 1),
('MASTER_CONTROL', 'MASTER_GOVERNANCE', 'menu.section.controls', 'MASTER', 10, 1),
('MASTER_ITEM', 'MASTER_PRODUCT', 'menu.master.item', 'MASTER', 10, 1),
('MASTER_BOM', 'MASTER_PRODUCT', 'menu.master.bom', 'MASTER', 20, 1),
('MASTER_CUSTOMER', 'MASTER_PARTNER', 'menu.master.customer', 'MASTER', 10, 1),
('MASTER_SUPPLIER', 'MASTER_PARTNER', 'menu.master.supplier', 'MASTER', 20, 1),
('MASTER_WAREHOUSE', 'MASTER_LOGISTICS', 'menu.master.warehouse', 'MASTER', 10, 1),
('MASTER_CHANGE_AUDIT', 'MASTER_CONTROL', 'menu.master.changeAudit', 'MASTER', 10, 1),
('PROCUREMENT_SOURCE', 'PROCUREMENT', 'menu.area.procurement.source', 'PROCUREMENT', 10, 1),
('PROCUREMENT_RECEIVE', 'PROCUREMENT', 'menu.area.procurement.receiving', 'PROCUREMENT', 20, 1),
('PROCUREMENT_BUYING', 'PROCUREMENT_SOURCE', 'menu.section.buying', 'PROCUREMENT', 10, 1),
('PROCUREMENT_INBOUND', 'PROCUREMENT_RECEIVE', 'menu.section.inbound', 'PROCUREMENT', 10, 1),
('PROCUREMENT_SETTLEMENT', 'PROCUREMENT_RECEIVE', 'menu.section.settlement', 'PROCUREMENT', 20, 1),
('PROCUREMENT_PR', 'PROCUREMENT_BUYING', 'menu.procurement.pr', 'PROCUREMENT', 10, 1),
('PROCUREMENT_PO', 'PROCUREMENT_BUYING', 'menu.procurement.po', 'PROCUREMENT', 20, 1),
('PROCUREMENT_RECEIPT', 'PROCUREMENT_INBOUND', 'menu.procurement.receipt', 'PROCUREMENT', 10, 1),
('PROCUREMENT_INVOICE', 'PROCUREMENT_SETTLEMENT', 'menu.procurement.invoice', 'PROCUREMENT', 10, 1),
('SALES_DOMESTIC', 'SALES', 'menu.area.sales.domestic', 'SALES', 10, 1),
('SALES_EXPORT_AREA', 'SALES', 'menu.area.sales.export', 'SALES', 20, 1),
('SALES_APPROVAL_AREA', 'SALES', 'menu.area.approval', 'SALES', 30, 1),
('SALES_ORDERING', 'SALES_DOMESTIC', 'menu.section.ordering', 'SALES', 10, 1),
('SALES_FULFILLMENT', 'SALES_DOMESTIC', 'menu.section.fulfillment', 'SALES', 20, 1),
('SALES_BILLING_SECTION', 'SALES_DOMESTIC', 'menu.section.billing', 'SALES', 30, 1),
('SALES_EXPORT_SECTION', 'SALES_EXPORT_AREA', 'menu.section.export', 'SALES', 10, 1),
('SALES_APPROVAL_SECTION', 'SALES_APPROVAL_AREA', 'menu.section.approval', 'SALES', 10, 1),
('SALES_QUOTATION', 'SALES_ORDERING', 'menu.sales.quotation', 'SALES', 10, 1),
('SALES_ORDER', 'SALES_ORDERING', 'menu.sales.order', 'SALES', 20, 1),
('SALES_SHIPMENT', 'SALES_FULFILLMENT', 'menu.sales.shipment', 'SALES', 10, 1),
('SALES_BILLING', 'SALES_BILLING_SECTION', 'menu.sales.billing', 'SALES', 10, 1),
('SALES_EXPORT_ORDER', 'SALES_EXPORT_SECTION', 'menu.sales.exportOrder', 'SALES', 10, 1),
('SALES_APPROVAL_REVIEW', 'SALES_APPROVAL_SECTION', 'menu.sales.approvalReview', 'SALES', 10, 1),
('INVENTORY_CONTROL', 'INVENTORY', 'menu.area.inventory.control', 'INVENTORY', 10, 1),
('INVENTORY_TRACE_AREA', 'INVENTORY', 'menu.area.inventory.trace', 'INVENTORY', 20, 1),
('INVENTORY_STOCK_SECTION', 'INVENTORY_CONTROL', 'menu.section.stock', 'INVENTORY', 10, 1),
('INVENTORY_TRACE_SECTION', 'INVENTORY_TRACE_AREA', 'menu.section.trace', 'INVENTORY', 10, 1),
('INVENTORY_COUNT_SECTION', 'INVENTORY_TRACE_AREA', 'menu.section.counting', 'INVENTORY', 20, 1),
('INVENTORY_STOCK', 'INVENTORY_STOCK_SECTION', 'menu.inventory.stock', 'INVENTORY', 10, 1),
('INVENTORY_TRANSFER', 'INVENTORY_STOCK_SECTION', 'menu.inventory.transfer', 'INVENTORY', 20, 1),
('INVENTORY_LOT', 'INVENTORY_TRACE_SECTION', 'menu.inventory.lot', 'INVENTORY', 10, 1),
('INVENTORY_COUNT', 'INVENTORY_COUNT_SECTION', 'menu.inventory.count', 'INVENTORY', 10, 1),
('MANUFACTURING_PLANNING', 'MANUFACTURING', 'menu.area.manufacturing.planning', 'MANUFACTURING', 10, 1),
('MANUFACTURING_EXECUTION', 'MANUFACTURING', 'menu.area.manufacturing.execution', 'MANUFACTURING', 20, 1),
('MANUFACTURING_PLAN_SECTION', 'MANUFACTURING_PLANNING', 'menu.section.planning', 'MANUFACTURING', 10, 1),
('MANUFACTURING_EXEC_SECTION', 'MANUFACTURING_EXECUTION', 'menu.section.execution', 'MANUFACTURING', 10, 1),
('MANUFACTURING_COST_SECTION', 'MANUFACTURING_EXECUTION', 'menu.section.costing', 'MANUFACTURING', 20, 1),
('MANUFACTURING_MRP', 'MANUFACTURING_PLAN_SECTION', 'menu.manufacturing.mrp', 'MANUFACTURING', 10, 1),
('MANUFACTURING_ORDER', 'MANUFACTURING_PLAN_SECTION', 'menu.manufacturing.order', 'MANUFACTURING', 20, 1),
('MANUFACTURING_ISSUE', 'MANUFACTURING_EXEC_SECTION', 'menu.manufacturing.issue', 'MANUFACTURING', 10, 1),
('MANUFACTURING_COST', 'MANUFACTURING_COST_SECTION', 'menu.manufacturing.cost', 'MANUFACTURING', 10, 1),
('FINANCE_ACCOUNTING', 'FINANCE', 'menu.area.finance.accounting', 'FINANCE', 10, 1),
('FINANCE_CLOSE_AREA', 'FINANCE', 'menu.area.finance.close', 'FINANCE', 20, 1),
('FINANCE_RECEIVABLES', 'FINANCE_ACCOUNTING', 'menu.section.receivables', 'FINANCE', 10, 1),
('FINANCE_PAYABLES', 'FINANCE_ACCOUNTING', 'menu.section.payables', 'FINANCE', 20, 1),
('FINANCE_GL_SECTION', 'FINANCE_CLOSE_AREA', 'menu.section.ledger', 'FINANCE', 10, 1),
('FINANCE_PERIOD_SECTION', 'FINANCE_CLOSE_AREA', 'menu.section.close', 'FINANCE', 20, 1),
('FINANCE_AR', 'FINANCE_RECEIVABLES', 'menu.finance.ar', 'FINANCE', 10, 1),
('FINANCE_AP', 'FINANCE_PAYABLES', 'menu.finance.ap', 'FINANCE', 10, 1),
('FINANCE_GL', 'FINANCE_GL_SECTION', 'menu.finance.gl', 'FINANCE', 10, 1),
('FINANCE_CLOSE', 'FINANCE_PERIOD_SECTION', 'menu.finance.close', 'FINANCE', 10, 1),
('AI_COPILOT', 'AI', 'menu.area.ai.copilot', 'AI', 10, 1),
('AI_ASSIST_SECTION', 'AI_COPILOT', 'menu.section.aiAssist', 'AI', 10, 1),
('AI_REPORT_SECTION', 'AI_COPILOT', 'menu.section.reports', 'AI', 20, 1),
('AI_QUERY', 'AI_ASSIST_SECTION', 'menu.ai.query', 'AI', 10, 1),
('AI_EXPLAIN', 'AI_ASSIST_SECTION', 'menu.ai.explain', 'AI', 20, 1),
('AI_SUMMARY', 'AI_REPORT_SECTION', 'menu.ai.summary', 'AI', 10, 1),
('ADMIN_SECURITY', 'ADMIN', 'menu.area.admin.security', 'ADMIN', 10, 1),
('ADMIN_AUDIT_AREA', 'ADMIN', 'menu.area.admin.audit', 'ADMIN', 20, 1),
('ADMIN_USER_SECTION', 'ADMIN_SECURITY', 'menu.section.users', 'ADMIN', 10, 1),
('ADMIN_ROLE_SECTION', 'ADMIN_SECURITY', 'menu.section.roles', 'ADMIN', 20, 1),
('ADMIN_AUDIT_SECTION', 'ADMIN_AUDIT_AREA', 'menu.section.audit', 'ADMIN', 10, 1),
('ADMIN_USERS', 'ADMIN_USER_SECTION', 'menu.admin.users', 'ADMIN', 10, 1),
('ADMIN_ROLES', 'ADMIN_ROLE_SECTION', 'menu.admin.roles', 'ADMIN', 10, 1),
('ADMIN_PERMISSIONS', 'ADMIN_ROLE_SECTION', 'menu.admin.permissions', 'ADMIN', 20, 1),
('ADMIN_LICENSE', 'ADMIN_ROLE_SECTION', 'menu.admin.license', 'ADMIN', 30, 1),
('ADMIN_AUDIT', 'ADMIN_AUDIT_SECTION', 'menu.admin.audit', 'ADMIN', 10, 1)
on duplicate key update parent_code = values(parent_code), name_key = values(name_key),
module_code = values(module_code), sort_order = values(sort_order), active = values(active);

insert into erp_item_masters (item_code, item_name, item_type, uom, plant, status, safety_stock, lead_time_days) values
('RM-1008', 'Servo motor 2kW', 'Purchased material', 'EA', 'JP01', 'status.released', '500', '14'),
('PK-2210', 'Export carton package', 'Packaging', 'EA', 'JP01', 'status.released', '1200', '7'),
('FG-3007', 'Smart actuator assembly', 'Finished good', 'EA', 'JP01', 'status.open', '80', '21')
on duplicate key update item_name = values(item_name), item_type = values(item_type), uom = values(uom),
plant = values(plant), status = values(status), safety_stock = values(safety_stock), lead_time_days = values(lead_time_days);

insert ignore into erp_role_menus (role_id, menu_id, can_view, can_create, can_update, can_approve)
select r.id, m.id, 1, 1, 1, 1
from erp_roles r join erp_menus m
where r.code = 'ADMIN';

insert ignore into erp_role_menus (role_id, menu_id, can_view, can_create, can_update, can_approve)
select r.id, m.id, 1,
       if(m.module_code in ('MASTER','ADMIN'), 0, 1),
       if(m.module_code = 'ADMIN', 0, 1),
       if(m.module_code in ('PROCUREMENT','MANUFACTURING'), 1, 0)
from erp_roles r join erp_menus m
where r.code = 'PLANNER';

insert into erp_module_actions (module_code, sort_order, action_key) values
('DASHBOARD', 10, 'action.refresh'), ('DASHBOARD', 20, 'action.simulate'), ('DASHBOARD', 30, 'action.export'),
('MASTER', 10, 'action.new'), ('MASTER', 20, 'action.edit'), ('MASTER', 30, 'action.export'),
('PROCUREMENT', 10, 'action.new'), ('PROCUREMENT', 20, 'action.approve'), ('PROCUREMENT', 30, 'action.post'), ('PROCUREMENT', 40, 'action.export'),
('SALES', 10, 'action.new'), ('SALES', 20, 'action.release'), ('SALES', 30, 'action.post'), ('SALES', 40, 'action.export'),
('INVENTORY', 10, 'action.new'), ('INVENTORY', 20, 'action.post'), ('INVENTORY', 30, 'action.simulate'), ('INVENTORY', 40, 'action.export'),
('MANUFACTURING', 10, 'action.new'), ('MANUFACTURING', 20, 'action.release'), ('MANUFACTURING', 30, 'action.simulate'), ('MANUFACTURING', 40, 'action.export'),
('FINANCE', 10, 'action.post'), ('FINANCE', 20, 'action.approve'), ('FINANCE', 30, 'action.export'),
('AI', 10, 'action.ask'), ('AI', 20, 'action.export'),
('ADMIN', 10, 'action.new'), ('ADMIN', 20, 'action.edit'), ('ADMIN', 30, 'action.approve'), ('ADMIN', 40, 'action.export')
on duplicate key update action_key = values(action_key);

insert into erp_module_process_steps (module_code, sort_order, label_value) values
('DASHBOARD', 10, 'flow.demand'), ('DASHBOARD', 20, 'flow.mrp'), ('DASHBOARD', 30, 'flow.purchase'), ('DASHBOARD', 40, 'flow.production'), ('DASHBOARD', 50, 'flow.goodsReceipt'), ('DASHBOARD', 60, 'flow.shipment'), ('DASHBOARD', 70, 'flow.billing'),
('MASTER', 10, 'term.organization'), ('MASTER', 20, 'term.itemMaster'), ('MASTER', 30, 'term.bom'), ('MASTER', 40, 'term.supplierMaster'), ('MASTER', 50, 'term.customerMaster'), ('MASTER', 60, 'term.warehouseMaster'),
('PROCUREMENT', 10, 'term.purchaseRequest'), ('PROCUREMENT', 20, 'term.purchaseOrder'), ('PROCUREMENT', 30, 'term.receipt'), ('PROCUREMENT', 40, 'term.invoiceCheck'), ('PROCUREMENT', 50, 'term.payable'),
('SALES', 10, 'term.quotation'), ('SALES', 20, 'term.salesOrder'), ('SALES', 30, 'term.delivery'), ('SALES', 40, 'term.shipment'), ('SALES', 50, 'term.billing'), ('SALES', 60, 'term.receivable'),
('INVENTORY', 10, 'term.stockOverview'), ('INVENTORY', 20, 'term.lotTrace'), ('INVENTORY', 30, 'term.transfer'), ('INVENTORY', 40, 'term.adjustment'), ('INVENTORY', 50, 'term.cycleCount'),
('MANUFACTURING', 10, 'term.bom'), ('MANUFACTURING', 20, 'term.routing'), ('MANUFACTURING', 30, 'term.mrpRun'), ('MANUFACTURING', 40, 'term.productionOrder'), ('MANUFACTURING', 50, 'term.materialIssue'), ('MANUFACTURING', 60, 'term.confirmation'), ('MANUFACTURING', 70, 'term.costing'),
('FINANCE', 10, 'term.ar'), ('FINANCE', 20, 'term.ap'), ('FINANCE', 30, 'term.inventoryValuation'), ('FINANCE', 40, 'term.costing'), ('FINANCE', 50, 'term.gl'), ('FINANCE', 60, 'term.close'),
('AI', 10, 'term.query'), ('AI', 20, 'term.explain'), ('AI', 30, 'term.summary'), ('AI', 40, 'term.workflow'),
('ADMIN', 10, 'term.users'), ('ADMIN', 20, 'term.roles'), ('ADMIN', 30, 'term.permissions'), ('ADMIN', 40, 'term.workflow'), ('ADMIN', 50, 'term.audit')
on duplicate key update label_value = values(label_value);

insert into erp_module_metrics (module_code, location, sort_order, label_value, metric_value, note_value, accent_code) values
('DASHBOARD', 'TOP', 10, 'dashboard.kpi.purchase', '12', 'status.waitingApproval', 'accent'),
('DASHBOARD', 'TOP', 20, 'dashboard.kpi.shortage', '5', 'status.shortage', 'error'),
('DASHBOARD', 'TOP', 30, 'dashboard.kpi.production', '8', 'status.released', 'success'),
('DASHBOARD', 'TOP', 40, 'dashboard.kpi.shipment', '16', 'status.open', 'accent'),
('MASTER', 'SIDE', 10, 'metric.service', '98.2%', null, 'success'),
('MASTER', 'SIDE', 20, 'metric.inventoryTurn', '7.4', null, 'accent'),
('MASTER', 'SIDE', 30, 'metric.otd', '94.8%', null, 'accent'),
('PROCUREMENT', 'SIDE', 10, 'status.late', '2', null, 'error'),
('PROCUREMENT', 'SIDE', 20, 'status.waitingApproval', '12', null, 'warning'),
('PROCUREMENT', 'SIDE', 30, 'term.payable', '$42,800', null, 'accent'),
('SALES', 'SIDE', 10, 'metric.otd', '94.8%', null, 'success'),
('SALES', 'SIDE', 20, 'term.receivable', '$318,400', null, 'accent'),
('SALES', 'SIDE', 30, 'status.blocked', '3', null, 'error'),
('INVENTORY', 'SIDE', 10, 'dashboard.kpi.shortage', '5', null, 'error'),
('INVENTORY', 'SIDE', 20, 'metric.inventoryTurn', '7.4', null, 'accent'),
('INVENTORY', 'SIDE', 30, 'term.lotTrace', '100%', null, 'success'),
('MANUFACTURING', 'SIDE', 10, 'dashboard.kpi.production', '8', null, 'accent'),
('MANUFACTURING', 'SIDE', 20, 'metric.costVariance', '+1.8%', null, 'warning'),
('MANUFACTURING', 'SIDE', 30, 'status.shortage', '5', null, 'error'),
('FINANCE', 'SIDE', 10, 'term.ar', '$318,400', null, 'accent'),
('FINANCE', 'SIDE', 20, 'term.ap', '$174,200', null, 'accent'),
('FINANCE', 'SIDE', 30, 'metric.costVariance', '+1.8%', null, 'warning'),
('AI', 'SIDE', 10, 'term.query', '42', null, 'accent'),
('AI', 'SIDE', 20, 'term.explain', '9', null, 'warning'),
('AI', 'SIDE', 30, 'term.summary', '7', null, 'success'),
('ADMIN', 'SIDE', 10, 'term.users', '24', null, 'accent'),
('ADMIN', 'SIDE', 20, 'term.roles', '8', null, 'accent'),
('ADMIN', 'SIDE', 30, 'term.audit', '1,280', null, 'success')
on duplicate key update label_value = values(label_value), metric_value = values(metric_value), note_value = values(note_value), accent_code = values(accent_code);

insert into erp_module_table_columns (module_code, sort_order, column_key) values
('DASHBOARD', 10, 'column.id'), ('DASHBOARD', 20, 'column.type'), ('DASHBOARD', 30, 'column.status'), ('DASHBOARD', 40, 'column.owner'), ('DASHBOARD', 50, 'column.next'),
('MASTER', 10, 'column.id'), ('MASTER', 20, 'column.name'), ('MASTER', 30, 'column.type'), ('MASTER', 40, 'column.plant'), ('MASTER', 50, 'column.status'), ('MASTER', 60, 'column.next'),
('PROCUREMENT', 10, 'column.id'), ('PROCUREMENT', 20, 'column.status'), ('PROCUREMENT', 30, 'column.item'), ('PROCUREMENT', 40, 'column.qty'), ('PROCUREMENT', 50, 'column.supplier'), ('PROCUREMENT', 60, 'column.due'), ('PROCUREMENT', 70, 'column.next'),
('SALES', 10, 'column.id'), ('SALES', 20, 'column.customer'), ('SALES', 30, 'column.amount'), ('SALES', 40, 'column.status'), ('SALES', 50, 'column.due'), ('SALES', 60, 'column.next'),
('INVENTORY', 10, 'column.item'), ('INVENTORY', 20, 'column.warehouse'), ('INVENTORY', 30, 'column.qty'), ('INVENTORY', 40, 'column.status'), ('INVENTORY', 50, 'column.risk'), ('INVENTORY', 60, 'column.next'),
('MANUFACTURING', 10, 'column.id'), ('MANUFACTURING', 20, 'column.item'), ('MANUFACTURING', 30, 'column.qty'), ('MANUFACTURING', 40, 'column.status'), ('MANUFACTURING', 50, 'column.due'), ('MANUFACTURING', 60, 'column.risk'), ('MANUFACTURING', 70, 'column.next'),
('FINANCE', 10, 'column.id'), ('FINANCE', 20, 'column.type'), ('FINANCE', 30, 'column.amount'), ('FINANCE', 40, 'column.status'), ('FINANCE', 50, 'column.date'), ('FINANCE', 60, 'column.next'),
('ADMIN', 10, 'column.id'), ('ADMIN', 20, 'column.name'), ('ADMIN', 30, 'column.type'), ('ADMIN', 40, 'column.owner'), ('ADMIN', 50, 'column.status'), ('ADMIN', 60, 'column.next')
on duplicate key update column_key = values(column_key);

insert into erp_module_table_rows (module_code, sort_order, c1, c2, c3, c4, c5, c6, c7) values
('DASHBOARD', 10, 'SO-2608-104', 'term.salesOrder', 'status.released', 'owner.sales', 'term.shipment', null, null),
('DASHBOARD', 20, 'MRP-2608-W34', 'term.mrpRun', 'status.shortage', 'owner.planner', 'term.purchaseRequest', null, null),
('DASHBOARD', 30, 'PO-45000127', 'term.purchaseOrder', 'status.late', 'owner.procurement', 'term.receipt', null, null),
('DASHBOARD', 40, 'MO-2608-004', 'term.productionOrder', 'status.blocked', 'owner.production', 'term.mrpRun', null, null),
('MASTER', 10, 'MAT-1008', 'Servo motor 2kW', 'term.itemMaster', 'JP01', 'status.released', 'action.edit', null),
('MASTER', 20, 'BOM-FG-220', 'Assembly BOM', 'term.bom', 'JP01', 'status.released', 'action.details', null),
('MASTER', 30, 'CUS-3001', 'Northwind Manufacturing', 'term.customerMaster', 'Global', 'status.open', 'action.edit', null),
('MASTER', 40, 'SUP-2007', 'Sakura Metals', 'term.supplierMaster', 'JP01', 'status.open', 'action.details', null),
('PROCUREMENT', 10, 'PR-2608-001', 'status.waitingApproval', 'RM-1008', '3,000', 'Sakura Metals', '2026-08-20', 'action.approve'),
('PROCUREMENT', 20, 'PO-45000127', 'status.late', 'PK-2210', '8,000', 'Kanto Package', '2026-08-18', 'action.details'),
('PROCUREMENT', 30, 'GR-50001988', 'status.ready', 'RM-1304', '1,200', 'Global Resin', '2026-08-17', 'action.post'),
('PROCUREMENT', 40, 'IV-81000312', 'status.open', 'RM-1008', '3,000', 'Sakura Metals', '2026-08-22', 'action.post'),
('SALES', 10, 'QT-2608-022', 'Northwind Manufacturing', '$86,000', 'status.draft', '2026-08-19', 'action.release', null),
('SALES', 20, 'SO-2608-104', 'Taiyo Robotics', '$124,600', 'status.released', '2026-08-21', 'term.delivery', null),
('SALES', 30, 'DN-2608-044', 'Apex Components', '$38,400', 'status.ready', '2026-08-17', 'term.shipment', null),
('SALES', 40, 'BI-2608-018', 'Northwind Manufacturing', '$52,800', 'status.open', '2026-08-23', 'action.post', null),
('INVENTORY', 10, 'RM-1008', 'WH-A', '420', 'status.shortage', 'risk.high', 'term.purchaseRequest', null),
('INVENTORY', 20, 'PK-2210', 'WH-B', '1,800', 'status.shortage', 'risk.medium', 'term.purchaseOrder', null),
('INVENTORY', 30, 'FG-3007', 'FG-01', '96', 'status.ready', 'risk.low', 'term.shipment', null),
('INVENTORY', 40, 'RM-1304', 'WH-A', '4,500', 'status.open', 'risk.low', 'term.cycleCount', null),
('MANUFACTURING', 10, 'MO-2608-004', 'FG-3007', '120', 'status.blocked', '2026-08-22', 'status.shortage', 'term.mrpRun'),
('MANUFACTURING', 20, 'MO-2608-005', 'FG-3041', '80', 'status.released', '2026-08-24', 'risk.low', 'term.materialIssue'),
('MANUFACTURING', 30, 'MO-2608-006', 'FG-3012', '160', 'status.ready', '2026-08-25', 'risk.low', 'term.confirmation'),
('MANUFACTURING', 40, 'PLN-2608-W34', 'term.weeklyPlan', '360', 'status.open', '2026-08-19', 'risk.medium', 'action.simulate'),
('FINANCE', 10, 'FI-900012', 'term.ar', '$52,800', 'status.open', '2026-08-17', 'action.post', null),
('FINANCE', 20, 'FI-900013', 'term.ap', '$24,600', 'status.waitingApproval', '2026-08-17', 'action.approve', null),
('FINANCE', 30, 'CO-700008', 'term.costing', '$8,420', 'status.ready', '2026-08-18', 'action.post', null),
('FINANCE', 40, 'GL-202608', 'term.close', '$1,284,000', 'status.open', '2026-08-31', 'action.details', null),
('ADMIN', 10, 'USR-admin', 'user.admin.name', 'term.users', 'IT', 'status.open', 'action.edit', null),
('ADMIN', 20, 'ROLE-PLN', 'role.planner', 'term.roles', 'PMO', 'status.released', 'term.permissions', null),
('ADMIN', 30, 'WF-PO-APP', 'term.purchaseOrder', 'term.workflow', 'owner.finance', 'status.open', 'action.approve', null),
('ADMIN', 40, 'AUD-2608', 'term.audit', 'term.audit', 'owner.system', 'status.ready', 'action.details', null)
on duplicate key update c1 = values(c1), c2 = values(c2), c3 = values(c3), c4 = values(c4), c5 = values(c5), c6 = values(c6), c7 = values(c7);

insert into erp_module_focus_items (module_code, sort_order, item_value) values
('DASHBOARD', 10, 'dashboard.alert.1'), ('DASHBOARD', 20, 'dashboard.alert.2'), ('DASHBOARD', 30, 'dashboard.alert.3'),
('MASTER', 10, 'focus.master.itemBom'), ('MASTER', 20, 'focus.master.permissions'), ('MASTER', 30, 'focus.master.audit'),
('PROCUREMENT', 10, 'dashboard.alert.2'), ('PROCUREMENT', 20, 'focus.procurement.poReceipt'), ('PROCUREMENT', 30, 'focus.procurement.invoicePayable'),
('SALES', 10, 'focus.sales.soShipment'), ('SALES', 20, 'focus.sales.billingReceivable'), ('SALES', 30, 'focus.sales.service'),
('INVENTORY', 10, 'dashboard.alert.3'), ('INVENTORY', 20, 'focus.inventory.transfer'), ('INVENTORY', 30, 'focus.inventory.cycle'),
('MANUFACTURING', 10, 'dashboard.alert.1'), ('MANUFACTURING', 20, 'focus.manufacturing.moIssue'), ('MANUFACTURING', 30, 'focus.manufacturing.costing'),
('FINANCE', 10, 'focus.finance.inventoryGl'), ('FINANCE', 20, 'focus.finance.close'), ('FINANCE', 30, 'focus.manufacturing.costing'),
('AI', 10, 'ai.answer'), ('AI', 20, 'dashboard.alert.1'), ('AI', 30, 'dashboard.alert.2'),
('ADMIN', 10, 'focus.admin.permissions'), ('ADMIN', 20, 'focus.admin.workflow'), ('ADMIN', 30, 'focus.admin.audit')
on duplicate key update item_value = values(item_value);

insert into erp_modules (code, title_key, subtitle_key, page_type, table_title_key, process_title_key, focus_title_key, prompt_value, sort_order, active) values
('REPORTS', 'module.reports', 'reports.subtitle', 'OPERATIONAL', 'table.worklist', 'panel.process', 'panel.todo', null, 75, 1)
on duplicate key update title_key = values(title_key), subtitle_key = values(subtitle_key), page_type = values(page_type),
table_title_key = values(table_title_key), process_title_key = values(process_title_key), focus_title_key = values(focus_title_key),
prompt_value = values(prompt_value), sort_order = values(sort_order), active = values(active);

insert into erp_menus (code, parent_code, name_key, module_code, sort_order, active) values
('REPORTS', null, 'module.reports', 'REPORTS', 75, 1),
('REPORTS_CORE', 'REPORTS', 'module.reports', 'REPORTS', 10, 1),
('REPORTS_BUSINESS', 'REPORTS_CORE', 'menu.section.reports', 'REPORTS', 10, 1),
('REPORT_SALES_DETAIL', 'REPORTS_BUSINESS', 'menu.report.salesDetail', 'REPORTS', 10, 1),
('REPORT_PURCHASE_DETAIL', 'REPORTS_BUSINESS', 'menu.report.purchaseDetail', 'REPORTS', 20, 1),
('REPORT_INVENTORY_DETAIL', 'REPORTS_BUSINESS', 'menu.report.inventoryDetail', 'REPORTS', 30, 1),
('REPORT_AR_BALANCE', 'REPORTS_BUSINESS', 'menu.report.arBalance', 'REPORTS', 40, 1),
('REPORT_AP_BALANCE', 'REPORTS_BUSINESS', 'menu.report.apBalance', 'REPORTS', 50, 1),
('PROCUREMENT_PO_QUERY', 'PROCUREMENT_BUYING', 'menu.procurement.poQuery', 'PROCUREMENT', 30, 1),
('PROCUREMENT_RECEIPT_QUERY', 'PROCUREMENT_INBOUND', 'menu.procurement.receiptQuery', 'PROCUREMENT', 20, 1),
('PROCUREMENT_CONFIRMATION', 'PROCUREMENT_SETTLEMENT', 'menu.procurement.confirmation', 'PROCUREMENT', 5, 1),
('PROCUREMENT_RETURN', 'PROCUREMENT_SETTLEMENT', 'menu.procurement.return', 'PROCUREMENT', 20, 1),
('SALES_ORDER_QUERY', 'SALES_ORDERING', 'menu.sales.orderQuery', 'SALES', 30, 1),
('SALES_SHIPMENT_QUERY', 'SALES_FULFILLMENT', 'menu.sales.shipmentQuery', 'SALES', 20, 1),
('SALES_CONFIRMATION', 'SALES_BILLING_SECTION', 'menu.sales.confirmation', 'SALES', 5, 1),
('SALES_RETURN', 'SALES_BILLING_SECTION', 'menu.sales.return', 'SALES', 20, 1),
('INVENTORY_LEDGER', 'INVENTORY_STOCK_SECTION', 'menu.inventory.ledger', 'INVENTORY', 15, 1),
('MANUFACTURING_ORDER_QUERY', 'MANUFACTURING_PLAN_SECTION', 'menu.manufacturing.orderQuery', 'MANUFACTURING', 30, 1),
('MANUFACTURING_COMPLETE', 'MANUFACTURING_EXEC_SECTION', 'menu.manufacturing.complete', 'MANUFACTURING', 20, 1),
('MANUFACTURING_RETURN', 'MANUFACTURING_EXEC_SECTION', 'menu.manufacturing.return', 'MANUFACTURING', 30, 1),
('FINANCE_COLLECTION', 'FINANCE_RECEIVABLES', 'menu.finance.collection', 'FINANCE', 20, 1),
('FINANCE_COLLECTION_QUERY', 'FINANCE_RECEIVABLES', 'menu.finance.collectionQuery', 'FINANCE', 30, 1),
('FINANCE_PAYMENT', 'FINANCE_PAYABLES', 'menu.finance.payment', 'FINANCE', 20, 1),
('FINANCE_PAYMENT_QUERY', 'FINANCE_PAYABLES', 'menu.finance.paymentQuery', 'FINANCE', 30, 1),
('ADMIN_LICENSE', 'ADMIN_ROLE_SECTION', 'menu.admin.license', 'ADMIN', 30, 1)
on duplicate key update parent_code = values(parent_code), name_key = values(name_key),
module_code = values(module_code), sort_order = values(sort_order), active = values(active);

insert ignore into erp_role_menus (role_id, menu_id, can_view, can_create, can_update, can_approve)
select r.id, m.id, 1, 1, 1, 1
from erp_roles r join erp_menus m
where r.code = 'ADMIN';

insert ignore into erp_role_menus (role_id, menu_id, can_view, can_create, can_update, can_approve)
select r.id, m.id, 1,
       if(m.module_code in ('MASTER','ADMIN','REPORTS'), 0, 1),
       if(m.module_code = 'ADMIN', 0, 1),
       if(m.module_code in ('PROCUREMENT','MANUFACTURING'), 1, 0)
from erp_roles r join erp_menus m
where r.code = 'PLANNER';

insert into erp_module_actions (module_code, sort_order, action_key) values
('REPORTS', 10, 'action.refresh'), ('REPORTS', 20, 'action.export')
on duplicate key update action_key = values(action_key);

insert into erp_module_process_steps (module_code, sort_order, label_value) values
('REPORTS', 10, 'term.salesOrder'), ('REPORTS', 20, 'term.purchaseOrder'), ('REPORTS', 30, 'term.stockOverview'),
('REPORTS', 40, 'term.ar'), ('REPORTS', 50, 'term.ap')
on duplicate key update label_value = values(label_value);

insert into erp_module_metrics (module_code, location, sort_order, label_value, metric_value, note_value, accent_code) values
('REPORTS', 'SIDE', 10, 'menu.report.salesDetail', '4', null, 'accent'),
('REPORTS', 'SIDE', 20, 'menu.report.inventoryDetail', '5', null, 'success'),
('REPORTS', 'SIDE', 30, 'menu.report.arBalance', '$318,400', null, 'warning')
on duplicate key update label_value = values(label_value), metric_value = values(metric_value), note_value = values(note_value), accent_code = values(accent_code);

insert into erp_module_table_columns (module_code, sort_order, column_key) values
('REPORTS', 10, 'column.id'), ('REPORTS', 20, 'column.type'), ('REPORTS', 30, 'column.amount'),
('REPORTS', 40, 'column.status'), ('REPORTS', 50, 'column.date'), ('REPORTS', 60, 'column.next')
on duplicate key update column_key = values(column_key);

insert into erp_module_table_rows (module_code, sort_order, c1, c2, c3, c4, c5, c6, c7) values
('REPORTS', 10, 'RPT-SALES-2608', 'menu.report.salesDetail', '$262,000', 'status.ready', '2026-08-31', 'action.export', null),
('REPORTS', 20, 'RPT-PUR-2608', 'menu.report.purchaseDetail', '$174,200', 'status.ready', '2026-08-31', 'action.export', null),
('REPORTS', 30, 'RPT-STK-2608', 'menu.report.inventoryDetail', '$1,284,000', 'status.open', '2026-08-31', 'action.refresh', null),
('REPORTS', 40, 'RPT-AR-2608', 'menu.report.arBalance', '$318,400', 'status.open', '2026-08-31', 'action.details', null),
('REPORTS', 50, 'RPT-AP-2608', 'menu.report.apBalance', '$174,200', 'status.open', '2026-08-31', 'action.details', null)
on duplicate key update c1 = values(c1), c2 = values(c2), c3 = values(c3), c4 = values(c4), c5 = values(c5), c6 = values(c6), c7 = values(c7);

insert into erp_module_focus_items (module_code, sort_order, item_value) values
('REPORTS', 10, 'menu.report.salesDetail'), ('REPORTS', 20, 'menu.report.inventoryDetail'), ('REPORTS', 30, 'menu.report.arBalance')
on duplicate key update item_value = values(item_value);
