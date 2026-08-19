update test set name='New Name2' where id=4;
create table migration.test6 (id integer generated always as identity, name varchar(100));
create table migration.test7 (id integer generated always as identity, name varchar(100));
create table migration.test8 (id integer generated always as identity, name varchar(100));
create table migration.test9 (id integer generated always as identity, name varchar(100));
create table migration.test10 (id integer generated always as identity, name varchar(100));
delete  FROM test where id=1;
