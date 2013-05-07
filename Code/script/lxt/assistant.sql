insert into XJJC_BD_FREEVALUEMAP (dr, pk_bdinfo,pk_corp,pk_freevalue,pk_freevaluemap,pk_usedfreevalue,vmemo,votherbiz,vothercode,vothername,obj_id)
(select 0, '00010000000000000073', '1022', bd_cubasdoc.pk_cubasdoc, 'XJJC'||dbms_random.string('u',16), 'XJJC0000000000000001', null, xj_amdb.view_payer.airport_code, xj_amdb.view_payer.iata_code,xj_amdb.view_payer.fname, xj_amdb.view_payer.obj_id from xj_amdb.view_payer, bd_cubasdoc
 where xj_amdb.view_payer.airlines='1' and bd_cubasdoc.custname=xj_amdb.view_payer.fname); --插入航空公司

insert into XJJC_BD_FREEVALUEMAP (dr, pk_bdinfo,pk_corp,pk_freevalue,pk_freevaluemap,pk_usedfreevalue,vmemo,votherbiz,vothercode,vothername,obj_id)
(select 0, '00010000000000000073', '1022', bd_cubasdoc.pk_cubasdoc, 'XJJC'||dbms_random.string('u',16), 'XJJC0000000000000004', null, xj_amdb.view_payer.airport_code, xj_amdb.view_payer.iata_code,xj_amdb.view_payer.fname, xj_amdb.view_payer.obj_id from xj_amdb.view_payer, bd_cubasdoc
 where xj_amdb.view_payer.airlines='0' and bd_cubasdoc.custname=xj_amdb.view_payer.fname); --插入客户

insert into XJJC_BD_FREEVALUEMAP (dr, pk_bdinfo,pk_corp,pk_freevalue,pk_freevaluemap,pk_usedfreevalue,vmemo,votherbiz,vothercode,vothername,obj_id)
(select 0, '00010000000000000002', '1022', bd_deptdoc.pk_deptdoc, 'XJJC'||dbms_random.string('u',16), 'XJJC0000000000000002', null, null, xj_amdb.view_airportunit.code,xj_amdb.view_airportunit.fname,xj_amdb.view_airportunit.obj_id from xj_amdb.view_airportunit, bd_deptdoc
 where bd_deptdoc.pk_corp='1022' and bd_deptdoc.deptname=xj_amdb.view_airportunit.fname||'机场'); --插入部门

insert into XJJC_BD_FREEVALUEMAP (dr, pk_bdinfo,pk_corp,pk_freevalue,pk_freevaluemap,pk_usedfreevalue,vmemo,votherbiz,vothercode,vothername,obj_id)
(select 0, '0001A11000000000022E', '1022', bd_jobbasfil.pk_jobbasfil, 'XJJC'||dbms_random.string('u',16), 'XJJC0000000000000003', null, xj_amdb.view_chargeproject.airport_code, xj_amdb.view_chargeproject.code,xj_amdb.view_chargeproject.cname, xj_amdb.view_chargeproject.obj_id from xj_amdb.view_chargeproject, bd_jobbasfil
 where bd_jobbasfil.pk_jobtype='0001A110000000000214' and (bd_jobbasfil.jobname=substr(xj_amdb.view_chargeproject.cname,5,instr(cname,'（',1,1)-5) or substr(bd_jobbasfil.jobname,1,2)=substr(xj_amdb.view_chargeproject.cname,5,instr(cname,'（',1,1)-5) or bd_jobbasfil.jobname=substr(xj_amdb.view_chargeproject.cname,1,instr(cname,'（',1,1)-1) or bd_jobbasfil.jobname=substr(xj_amdb.view_chargeproject.cname,3,instr(cname,'（',1,1)-3)) 
 and (instr(xj_amdb.view_chargeproject.cname,'车',1,1)<>0 and xj_amdb.view_chargeproject.code <>'ZZZZ')); --插入车型

insert into XJJC_BD_SUBJMAP(bmergesame,currency,dr,obj_id,pk_corp,pk_creditsubj,pk_debitsubj,pk_subjbiz,pk_subjmap,vmemo,votherbiz,vothercode,vothername)
(select 'Y', xj_amdb.view_chargeproject.currency,0,xj_amdb.view_chargeproject.obj_id,'1022', bd_accsubj.pk_accsubj, '0001A11000000000150F', 'XJJC0000000000000006', 
 'XJJC'||dbms_random.string('u',16), null, xj_amdb.view_chargeproject.airport_code, xj_amdb.view_chargeproject.code, xj_amdb.view_chargeproject.cname 
 from xj_amdb.view_chargeproject, bd_accsubj, xjjc_code_to_code
 where xj_amdb.view_chargeproject.code=xjjc_code_to_code.othercode and xjjc_code_to_code.nccode=bd_accsubj.subjcode and xjjc_code_to_code.type='AIRINCOMESUBJ' 
 and xj_amdb.view_chargeproject.code <>'ZZZZ' and bd_accsubj.pk_glorgbook='0001A110000000000394') --插入航空收入科目
 

select distinct iata_code, fname
  from xj_amdb.view_payer
 where xj_amdb.view_payer.iata_code in
       (select distinct payer_code
          from xj_amdb.view_invoice
         where payer_code not in
               (select vothercode from xjjc_bd_freevaluemap)); --查找未对应航空公司

select distinct iata_code, fname
  from xj_amdb.view_payer
 where xj_amdb.view_payer.iata_code in
       (select distinct payer_code
          from xj_amdb.view_rent_contract
         where payer_code not in
               (select vothercode from xjjc_bd_freevaluemap)); --查找未对应客户

select distinct code, cname, substr(cname, 1, instr(cname, '（', 1, 1) - 1)
  from xj_amdb.view_chargeproject
 where instr(cname, '车', 1, 1) <> 0
   and code <> 'ZZZZ'
   and code not in (select vothercode from xjjc_bd_freevaluemap); --查找未对应车型

select distinct xj_amdb.view_chargeproject.code,
                xj_amdb.view_chargeproject.cname
  from xj_amdb.view_chargeproject, xj_amdb.view_invoice_item
 where xj_amdb.view_chargeproject.obj_id =
       xj_amdb.view_invoice_item.charge_project_id
   and xj_amdb.view_chargeproject.airport_code =
       xj_amdb.view_invoice_item.airport_code
   and xj_amdb.view_chargeproject.code not in
       (select othercode from xjjc_code_to_code)
 order by xj_amdb.view_chargeproject.code;                 --查找未对应航空收入科目，在编码对照中