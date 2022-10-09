create table post
(
    id               uuid PRIMARY KEY not null,
    title            text             not null,
    summary          text             not null,
    original_content text             not null,
    html_content     text             not null,
    friendly_url     text             not null,
    created_at       timestamp        not null
);

create index post_created_at_idx on post (created_at);
create unique index post_friendly_url_uq on post (friendly_url);