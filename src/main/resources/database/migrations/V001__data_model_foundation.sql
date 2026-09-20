create table if not exists schema_version (
  version varchar(80) primary key,
  description varchar(255),
  installed_at timestamp not null default current_timestamp
) engine=InnoDB default charset=utf8mb4;
