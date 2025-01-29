CREATE TABLE "codeforces_problemsets"
(
    contestId          integer not null,
    "index"            TEXT    not null,
    name               TEXT    not null,
    type               TEXT    not null,
    points             REAL,
    tags               TEXT,
    solvedCount        integer,
    lastUpdateDateTime INTEGER,
    id                 INTEGER not null
        constraint codeforces_problemsets_pk
            primary key
);
CREATE INDEX codeforces_problemsets_contestId_index_index
    on codeforces_problemsets (contestId, "index");

CREATE VIRTUAL TABLE codeforces_tags_fts using fts5
(
    tags,
    content='codeforces_problemsets',
    content_rowid='id'
);

create trigger codeforces_problemsets_ai
    after insert
    on codeforces_problemsets
begin
    insert into codeforces_tags_fts(rowid, tags) values (new.id, new.tags);
end;

create trigger codeforces_problemsets_ad
    after delete
    on codeforces_problemsets
begin
    insert into codeforces_tags_fts(codeforces_tags_fts, rowid, tags) values ('delete', old.id, old.tags);
end;

create trigger codeforces_problemsets_au
    after update
    on codeforces_problemsets
begin
    insert into codeforces_tags_fts(codeforces_tags_fts, rowid, tags) values ('delete', old.id, old.tags);
    insert into codeforces_tags_fts(rowid, tags) values (new.id, new.tags);
end;