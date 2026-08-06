create function get_test_data()
returns table(id integer, name varchar(100))
language sql
as $$
	select id, name from test;
$$;
