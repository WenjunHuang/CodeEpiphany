create table codeforces_problemsets
(
    id                 INTEGER not null
        constraint codeforces_problemsets_pk primary key,
    contestId          INTEGER,
    solvedCount        INTEGER,
    lastUpdateDateTime INTEGER,
    rating             INTEGER,
    points             REAL,
    "index"            TEXT    not null,
    name               TEXT    not null,
    type               TEXT    not null,
    contestIdIndex     TEXT    not null,
    tags               TEXT,
    problemsetName     TEXT
);

create index codeforces_problemsets_contestId_index_index
    on codeforces_problemsets (contestId, "index");


CREATE VIRTUAL TABLE codeforces_problemsets_fts using fts5
(
    id UNINDEXED,
    contestId,
    solvedCount UNINDEXED,
    lastUpdateDateTime UNINDEXED,
    rating,
    points UNINDEXED,
    "index",
    name,
    type,
    contestIdIndex,
    tags,
    problemsetName,
    content='codeforces_problemsets',
    content_rowid='id'
);

create trigger codeforces_problemsets_ai
    after insert
    on codeforces_problemsets
begin
    insert into codeforces_problemsets_fts(rowid, id,contestId,solvedCount,lastUpdateDateTime,rating,points,"index",name,type,contestIdIndex,tags,problemsetName)
    values (new.id, new.id, new.contestId, new.solvedCount, new.lastUpdateDateTime, new.rating, new.points, new."index", new.name, new.type, new.contestIdIndex, new.tags, new.problemsetName);
end;

create trigger codeforces_problemsets_au
    after update
    on codeforces_problemsets
begin
    insert into codeforces_problemsets_fts(codeforces_problemsets_fts,rowid, id,contestId,solvedCount,lastUpdateDateTime,rating,points,"index",name,type,contestIdIndex,tags,problemsetName)
    values ('delete',old.id, old.id, old.contestId, old.solvedCount, old.lastUpdateDateTime, old.rating, old.points, old."index", old.name, old.type, old.contestIdIndex, old.tags, old.problemsetName);
    insert into codeforces_problemsets_fts(rowid, id,contestId,solvedCount,lastUpdateDateTime,rating,points,"index",name,type,contestIdIndex,tags,problemsetName)
    values (new.id, new.id, new.contestId, new.solvedCount, new.lastUpdateDateTime, new.rating, new.points, new."index", new.name, new.type, new.contestIdIndex, new.tags, new.problemsetName);
end;

create trigger codeforces_problemsets_ad
    after delete
    on codeforces_problemsets
begin
    insert into codeforces_problemsets_fts(codeforces_problemsets_fts,rowid, id,contestId,solvedCount,lastUpdateDateTime,rating,points,"index",name,type,contestIdIndex,tags,problemsetName)
    values ('delete',old.id, old.id, old.contestId, old.solvedCount, old.lastUpdateDateTime, old.rating, old.points, old."index", old.name, old.type, old.contestIdIndex, old.tags, old.problemsetName);
end;
