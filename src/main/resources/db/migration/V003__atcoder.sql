CREATE TABLE "atcoder_contests"
(
    contestId        TEXT    not null,
    startEpochSecond integer,
    durationSecond   integer,
    title            TEXT    not null,
    rateChange       TEXT,
    id               integer not null
        constraint atcoder_contests_pk
            primary key
);
CREATE VIRTUAL TABLE atcoder_contests_fts using fts5(
    id UNINDEXED,
    contestId,
    startEpochSecond UNINDEXED,
    durationSecond UNINDEXED,
    title,
    rateChange UNINDEXED,
    tokenize="trigram",
    content='atcoder_contests',
    content_rowid='id');
CREATE TABLE "atcoder_problems"
(
    problemId            TEXT    not null,
    contestId            TEXT    not null,
    problemIndex         TEXT    not null,
    name                 TEXT    not null,
    title                TEXT    not null,
    shortestSubmissionId INTEGER,
    shortestContestId    TEXT,
    shortestUserId       TEXT,
    fastestSubmissionId  INTEGER,
    fastestContestId     TEXT,
    fastestUserId        TEXT,
    firstSubmissionId    INTEGER,
    firstContestId       TEXT,
    firstUserId          TEXT,
    solverCount          integer,
    id                   integer not null
        constraint atcoder_problems_pk
            primary key
);
CREATE VIRTUAL TABLE atcoder_problems_fts using fts5(
    id UNINDEXED,
    problemId,
    contestId,
    problemIndex,
    name,
    title,
    shortestSubmissionId UNINDEXED,
    shortestContestId UNINDEXED,
    shortestUserId UNINDEXED,
    fastestSubmissionId UNINDEXED,
    fastestContestId UNINDEXED,
    fastestUserId UNINDEXED,
    firstSubmissionId UNINDEXED,
    firstContestId UNINDEXED,
    firstUserId UNINDEXED,
    solverCount UNINDEXED,
    tokenize='trigram',
    content='atcoder_problems',
    content_rowid='id'
);
CREATE TRIGGER atcoder_contests_ad
    after delete
    on atcoder_contests
begin
    insert into atcoder_contests_fts(atcoder_contests_fts,rowid, id,contestId,startEpochSecond,durationSecond,title,rateChange)
    values ('delete',old.id, old.id,old.contestId,old.startEpochSecond,old.durationSecond,old.title,old.rateChange);
end;

CREATE TRIGGER atcoder_contests_ai
    after insert
    on atcoder_contests
begin
    insert into atcoder_contests_fts(rowid, id,contestId,startEpochSecond,durationSecond,title,rateChange)
    values (new.id, new.id,new.contestId,new.startEpochSecond,new.durationSecond,new.title,new.rateChange);
end;

CREATE TRIGGER atcoder_contests_au
    after update
    on atcoder_contests
begin
    insert into atcoder_contests_fts(atcoder_contests_fts,rowid, id,contestId,startEpochSecond,durationSecond,title,rateChange)
    values ('delete',old.id, old.id,old.contestId,old.startEpochSecond,old.durationSecond,old.title,old.rateChange);
    insert into atcoder_contests_fts(rowid, id,contestId,startEpochSecond,durationSecond,title,rateChange)
    values (new.id, new.id,new.contestId,new.startEpochSecond,new.durationSecond,new.title,new.rateChange);
end;

CREATE TRIGGER atcoder_problems_ad
    after delete
    on atcoder_problems
begin
    insert into  atcoder_problems_fts(atcoder_problems_fts,rowid, id,problemId,contestId,problemIndex,name,title,shortestSubmissionId,shortestContestId,shortestUserId,fastestSubmissionId,fastestUserId,firstSubmissionId,firstContestId,firstUserId,solverCount)
    values ('delete',old.id,old.id,old.problemId,old.contestId,old.problemIndex,old.name,old.title,old.shortestSubmissionId,old.shortestContestId,old.shortestUserId,old.fastestSubmissionId,old.fastestUserId,old.firstSubmissionId,old.firstContestId,old.firstUserId,old.solverCount);
end;

CREATE TRIGGER atcoder_problems_ai
    after insert
    on atcoder_problems
begin
    insert into  atcoder_problems_fts(rowid, id,problemId,contestId,problemIndex,name,title,shortestSubmissionId,shortestContestId,shortestUserId,fastestSubmissionId,fastestUserId,firstSubmissionId,firstContestId,firstUserId,solverCount)
    values (new.id,new.id,new.problemId,new.contestId,new.problemIndex,new.name,new.title,new.shortestSubmissionId,new.shortestContestId,new.shortestUserId,new.fastestSubmissionId,new.fastestUserId,new.firstSubmissionId,new.firstContestId,new.firstUserId,new.solverCount);
end;

CREATE TRIGGER atcoder_problems_au
    after update
    on atcoder_problems
begin
    insert into  atcoder_problems_fts(atcoder_problems_fts,rowid, id,problemId,contestId,problemIndex,name,title,shortestSubmissionId,shortestContestId,shortestUserId,fastestSubmissionId,fastestUserId,firstSubmissionId,firstContestId,firstUserId,solverCount)
    values ('delete',old.id,old.id,old.problemId,old.contestId,old.problemIndex,old.name,old.title,old.shortestSubmissionId,old.shortestContestId,old.shortestUserId,old.fastestSubmissionId,old.fastestUserId,old.firstSubmissionId,old.firstContestId,old.firstUserId,old.solverCount);
    insert into  atcoder_problems_fts(rowid, id,problemId,contestId,problemIndex,name,title,shortestSubmissionId,shortestContestId,shortestUserId,fastestSubmissionId,fastestUserId,firstSubmissionId,firstContestId,firstUserId,solverCount)
    values (new.id,new.id,new.problemId,new.contestId,new.problemIndex,new.name,new.title,new.shortestSubmissionId,new.shortestContestId,new.shortestUserId,new.fastestSubmissionId,new.fastestUserId,new.firstSubmissionId,new.firstContestId,new.firstUserId,new.solverCount);
end;

