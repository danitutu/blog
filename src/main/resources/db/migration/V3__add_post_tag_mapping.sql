create table post_tag
(
    post_id  uuid not null references post (id),
    tag_name text not null references tag (name),
    primary key (post_id, tag_name)
);
