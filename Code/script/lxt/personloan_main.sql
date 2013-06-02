----------------------------------------------------
-- 表结构建立                                     --
----------------------------------------------------
create table XJJC_GL_PERSONLOAN
(
  pk_psnbasdoc CHAR(20) not null,
  nloan        NUMBER(28,8) not null,
  ts           CHAR(19) default to_char(sysdate,'yyyy-mm-dd hh24:mi:ss'),
  dr           NUMBER(10) default 0
)
;
alter table XJJC_GL_PERSONLOAN
  add constraint PK_XJJC_PSNBASDOC primary key (PK_PSNBASDOC);