
create table Note (
    id          bigserial   primary key,
    username    text        not null,
    title       text        not null,
    body        text        not null
);
