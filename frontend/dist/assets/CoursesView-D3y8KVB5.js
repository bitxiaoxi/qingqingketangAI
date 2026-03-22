import{_ as nt,k as M,i as se,o as lt,a as ct,b as k,c as D,d as V,f as O,g as ge,e as ze,u as ne,s as dt,q as Ae,F as ft,r as ut,h as le,n as He,l as U,j as b,H as ht,t as gt,E as We,I as pt,J as mt,K as bt,L as $t,C as yt}from"./index-D9jOgdgW.js";import{a as pe}from"./api-Dao-lkRU.js";const xt={class:"page-stack"},kt={key:0,class:"page-state schedule-board-state"},wt={key:2,class:"schedule-poster-section"},vt={class:"schedule-poster-toolbar"},Lt=["data-state","data-size","disabled","aria-label","onClick"],St=7*24*60*60*1e3,Ee=1600,ce=10,Mt={__name:"CoursesView",setup(Tt){const v=M(!1),z=M(""),A=M([]),p=M(se(new Date)),$=M(""),Z=M(null),me=M(1);let X=0,H=null;const g=t=>String(t??"").replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;").replace(/'/g,"&#39;"),Y=t=>Array.from(String(t??"")).reduce((e,a)=>e+(/[\u0000-\u00ff]/.test(a)?.55:1),0),q=(t,e)=>{const a=String(t??"");if(Y(a)<=e)return a;let r="";for(const o of Array.from(a)){const i=`${r}${o}`;if(Y(`${i}…`)>e)break;r=i}return`${r}…`},Be=(t,e,a)=>{const r=(t??[]).map(s=>String(s??"").trim()).filter(Boolean);if(!r.length||e<=0||a<=0)return[];const o=[];let i=0;for(;i<r.length&&o.length<a;){let s="";for(;i<r.length;){const n=s?`、${r[i]}`:r[i];if(Y(`${s}${n}`)<=e){s+=n,i+=1;continue}s||(s=q(r[i],e),i+=1);break}if(!s)break;o.push(s)}if(i<r.length&&o.length){const s=`等${r.length}位`,n=Math.max(3,e-Y(s));o[o.length-1]=`${q(o[o.length-1],n)}${s}`}return o},W=t=>{const e=new Date(t);return e.getHours()*60+e.getMinutes()},Ge=t=>{const e=Math.floor(t/60);return`${String(e).padStart(2,"0")}:00`},Pe=t=>`data:image/svg+xml;charset=utf-8,${encodeURIComponent(t)}`,be=()=>{var e;const t=((e=Z.value)==null?void 0:e.clientWidth)??Ee;me.value=Math.min(1,Math.max(.35,t/Ee))},Re=()=>{if(!T.value||typeof window>"u"||typeof document>"u")return;const t=document.createElement("iframe");t.setAttribute("aria-hidden","true"),t.style.position="fixed",t.style.width="0",t.style.height="0",t.style.border="0",t.style.right="0",t.style.bottom="0",document.body.appendChild(t);const e=()=>{window.setTimeout(()=>{document.body.contains(t)&&document.body.removeChild(t)},300)},a=t.contentWindow;if(!a){e();return}const r=g(`${xe.value} ${$e.value}`);a.document.open(),a.document.write(`
    <!doctype html>
    <html lang="zh-CN">
      <head>
        <meta charset="UTF-8" />
        <title>${r}</title>
        <style>
          @page {
            size: landscape;
            margin: 6mm;
          }

          html,
          body {
            margin: 0;
            padding: 0;
            background: #ffffff;
            width: 100%;
            height: 100%;
            overflow: hidden;
            -webkit-print-color-adjust: exact;
            print-color-adjust: exact;
          }

          .print-sheet {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 100vw;
            height: 100vh;
            overflow: hidden;
          }

          img {
            display: block;
            width: auto;
            height: auto;
            max-width: 100%;
            max-height: 100%;
            object-fit: contain;
            break-inside: avoid;
            page-break-inside: avoid;
          }
        </style>
      </head>
      <body>
        <div class="print-sheet">
          <img id="poster" src="${T.value}" alt="${r}" />
        </div>
      </body>
    </html>
  `),a.document.close();const o=a.document.getElementById("poster"),i=()=>{a.focus(),a.print()};if(a.onafterprint=e,o!=null&&o.complete){window.setTimeout(i,120);return}o==null||o.addEventListener("load",()=>{window.setTimeout(i,120)},{once:!0}),o==null||o.addEventListener("error",e,{once:!0})},F=async(t=p.value)=>{const e=++X;v.value=!0,z.value="",A.value=[];try{const a=await pe.listWeekSchedules(le(t));if(e!==X)return;A.value=a??[]}catch(a){if(e!==X)return;z.value=He(a,"课表加载失败")}finally{e===X&&(v.value=!1)}},je=(t,e)=>{const a=new Set((t??[]).map(r=>String(r)));A.value=(A.value??[]).map(r=>a.has(String(r.id))?{...r,status:e}:r)},Ke=b(()=>(A.value??[]).slice().sort((t,e)=>new Date(t.startTime)-new Date(e.startTime)).map(t=>{const e=String(t.status??"").toUpperCase()==="COMPLETED";return{...t,scheduleId:t.id,studentName:t.studentName??"未命名",subject:t.subject??"正式课",timeRange:yt(t.startTime,t.endTime),isCompleted:e}})),L=b(()=>{const t=new Map;return Ke.value.forEach(e=>{const a=le(e.startTime),r=[a,e.startTime,e.endTime,e.subject].join("|"),o=t.get(r);if(o){o.scheduleIds.push(e.scheduleId),o.studentIds.add(e.studentId??e.scheduleId),o.studentNames.add(e.studentName),e.isCompleted?o.completedScheduleIds.push(e.scheduleId):o.pendingScheduleIds.push(e.scheduleId);return}t.set(r,{groupKey:r,dateKey:a,startTime:e.startTime,endTime:e.endTime,sortTime:new Date(e.startTime).getTime(),timeRange:e.timeRange,subject:e.subject,scheduleIds:[e.scheduleId],completedScheduleIds:e.isCompleted?[e.scheduleId]:[],pendingScheduleIds:e.isCompleted?[]:[e.scheduleId],studentIds:new Set([e.studentId??e.scheduleId]),studentNames:new Set([e.studentName])})}),Array.from(t.values()).map(e=>{const a=Array.from(e.studentNames),r=e.studentIds.size||e.scheduleIds.length,o=e.completedScheduleIds.length,i=e.pendingScheduleIds.length,s=o===e.scheduleIds.length,n=o>0&&i>0;return{groupKey:e.groupKey,dateKey:e.dateKey,startTime:e.startTime,endTime:e.endTime,sortTime:e.sortTime,timeRange:e.timeRange,subject:e.subject,scheduleIds:e.scheduleIds,completedScheduleIds:e.completedScheduleIds,pendingScheduleIds:e.pendingScheduleIds,participantCount:r,participantLabel:`共 ${r} 位学员`,studentNames:a,studentNamesLabel:a.join("、"),completedCount:o,pendingCount:i,isCompleted:s,isPartial:n,sessionNote:r>1?"小组课":"单人课"}}).sort((e,a)=>e.sortTime-a.sortTime)}),Q=b(()=>{const t=le(new Date);return mt(p.value).map(e=>{const a=le(e),r=e.getDay(),o=L.value.filter(i=>i.dateKey===a);return{dateKey:a,weekday:$t(e),dateLabel:bt(e),dateNumber:`${e.getDate()}`.padStart(2,"0"),monthLabel:`${e.getMonth()+1}月`,isToday:a===t,isWeekend:r===0||r===6,items:o}})}),$e=b(()=>pt(p.value)),Ve=b(()=>{const t=p.value?p.value.getTime():0,e=se(new Date).getTime();return Math.round((t-e)/St)}),ye=b(()=>{const t=Ve.value;return t===0?"本周":t===-1?"上周":t===1?"下周":t>0?`${t} 周后`:`${Math.abs(t)} 周前`}),xe=b(()=>`${ye.value}课程表`),ke=t=>t.isCompleted?"已销课":"待销课",Oe=t=>t.isCompleted?`取消 ${t.timeRange} ${t.subject} 的销课`:t.isPartial?`继续为 ${t.timeRange} ${t.subject} 销课`:`为 ${t.timeRange} ${t.subject} 销课`,we=async t=>{const e=new Date(p.value);e.setDate(e.getDate()+t*7),p.value=se(e),await F(p.value)},Ue=async()=>{p.value=se(new Date),await F(p.value)},Ze=async t=>{if(!t||$.value)return;const e=!t.isCompleted,a=e?t.pendingScheduleIds:t.completedScheduleIds;if(a.length){$.value=t.groupKey;try{for(const r of a)e?await pe.completeSchedule(r):await pe.undoCompleteSchedule(r);je(a,e?"COMPLETED":"PLANNED"),We.success(e?"已销课":"已恢复为待上课")}catch(r){We.error(He(r,e?"销课失败":"恢复待上课失败")),await F(p.value)}finally{$.value=""}}},ve=()=>{if(!Q.value.length)return null;const t=1600,e=36,a=122,r=12,o=Math.floor((t-e*2-a-r*6)/7),i=162,s=78,n=38,J=8*60,ee=22*60;let w=J,f=ee;if(L.value.length){const R=Math.min(...L.value.map(K=>W(K.startTime))),j=Math.max(...L.value.map(K=>W(K.endTime)));w=Math.max(7*60,Math.floor(R/60)*60-60),f=Math.min(23*60,Math.ceil(j/60)*60+60),f-w<8*60&&(f=Math.min(23*60,w+8*60))}const te=Math.max(1,Math.ceil((f-w)/60)),re=84,m=e+i+s,S=te*re,I=m+S,ae=I+n,E=e+a,C=e+i,B=new Map(Q.value.map((R,j)=>[R.dateKey,j])),G=7*o+6*r,_=t-e*2,oe=i-24,ie=e+24,P=C-16,de=I-P,fe=E-24,ue=e+12,he=a-42;return{width:t,height:ae,outerPadding:e,leftRail:a,columnGap:r,columnWidth:o,headerHeight:i,dayHeaderHeight:s,bottomPadding:n,visibleStart:w,visibleEnd:f,hourRows:te,hourHeight:re,gridTop:m,gridHeight:S,gridBottom:I,gridLeft:E,dayHeaderTop:C,dayIndexMap:B,gridAreaWidth:G,titlePanelWidth:_,titlePanelHeight:oe,titleTextX:ie,boardPanelY:P,boardPanelHeight:de,timelineAxisX:fe,timelineLabelX:ue,timelineLabelWidth:he,timelineLabelHeight:28}},Xe=t=>t.isCompleted?{fill:"#f0fdf4",stroke:"#86efac",accent:"#22c55e",badge:"#dcfce7",badgeText:"#15803d"}:t.isPartial?{fill:"#eff6ff",stroke:"#93c5fd",accent:"#3b82f6",badge:"#dbeafe",badgeText:"#1d4ed8"}:{fill:"#fff7ed",stroke:"#fdba74",accent:"#f59e0b",badge:"#ffedd5",badgeText:"#c2410c"},Le=t=>t<=118?{size:"compact",width:52,height:20,reserve:66,fontSize:10}:{size:"default",width:60,height:24,reserve:74,fontSize:11},Se=(t,e)=>{const a=e.dayIndexMap.get(t.dateKey);if(a===void 0)return null;const r=e.gridLeft+a*(e.columnWidth+e.columnGap)+10,o=e.gridTop+(W(t.startTime)-e.visibleStart)/60*e.hourHeight+8,i=e.columnWidth-20,s=Math.max(76,(W(t.endTime)-W(t.startTime))/60*e.hourHeight-12);return{x:r,y:o,width:i,height:s}},Me=b(()=>{const t=ve();return t?L.value.map(e=>{const a=Se(e,t);if(!a)return null;const r=Le(a.height);return{...e,label:ke(e),ariaLabel:Oe(e),state:e.isCompleted?"completed":e.isPartial?"partial":"pending",size:r.size,style:{top:`${(a.y+ce)/t.height*100}%`,left:`${(a.x+a.width-ce)/t.width*100}%`}}}).filter(Boolean):[]}),Ye=b(()=>({"--schedule-action-scale":`${me.value}`})),T=b(()=>{const t=ve();if(!t)return"";const{width:e,height:a,outerPadding:r,leftRail:o,columnGap:i,columnWidth:s,dayHeaderHeight:n,visibleStart:J,hourRows:ee,hourHeight:w,gridTop:f,gridHeight:te,gridBottom:re,gridLeft:m,dayHeaderTop:S,gridAreaWidth:I,titlePanelWidth:ae,titlePanelHeight:E,titleTextX:C,boardPanelY:B,boardPanelHeight:G,timelineAxisX:_,timelineLabelX:oe,timelineLabelWidth:ie,timelineLabelHeight:P}=t,de=Q.value.map((l,d)=>{const c=m+d*(s+i),u=l.isToday?"#dbeafe":l.isWeekend?"#f0f9ff":"#ffffff",h=l.isToday?"#60a5fa":"#dbe7f3",y=l.items.length?`${l.items.length} 节课`:"空档";return`
      <g>
        <rect x="${c}" y="${S}" width="${s}" height="${n-12}" rx="22" fill="${u}" stroke="${h}" />
        <text x="${c+18}" y="${S+30}" font-size="16" font-weight="700" fill="#0f172a">${g(l.weekday)}</text>
        <text x="${c+18}" y="${S+54}" font-size="12" fill="#64748b">${g(`${l.monthLabel} ${l.dateNumber}`)}</text>
        <text x="${c+s-18}" y="${S+54}" text-anchor="end" font-size="12" fill="${l.isToday?"#2563eb":"#94a3b8"}">${g(l.isToday?"今天":y)}</text>
      </g>
    `}).join(""),fe=Q.value.map((l,d)=>`<rect x="${m+d*(s+i)}" y="${f}" width="${s}" height="${te}" rx="26" fill="${l.isWeekend?"#f8fcff":"#ffffff"}" stroke="#e5edf5" />`).join(""),ue=Array.from({length:ee+1},(l,d)=>{const c=f+d*w;return`
      <line x1="${m}" y1="${c}" x2="${m+7*s+6*i}" y2="${c}" stroke="#e8eef5" />
    `}).join(""),he=`
    <g>
      <line x1="${_}" y1="${f+14}" x2="${_}" y2="${re-14}" stroke="rgba(148,163,184,0.38)" stroke-width="3" stroke-linecap="round" />
      ${Array.from({length:ee},(l,d)=>{const u=f+d*w+8,h=u+P/2,y=_+10,N=m-8;return`
          <g>
            <rect x="${oe}" y="${u}" width="${ie}" height="${P}" rx="14" fill="rgba(255,255,255,0.92)" stroke="rgba(191,219,254,0.92)" />
            <text x="${oe+ie/2}" y="${u+18}" text-anchor="middle" font-size="12" font-weight="700" fill="#475569">${g(Ge(J+d*60))}</text>
            <circle cx="${_}" cy="${h}" r="5" fill="#ffffff" stroke="#93c5fd" stroke-width="3" />
            <line x1="${y}" y1="${h}" x2="${N}" y2="${h}" stroke="rgba(191,219,254,0.78)" stroke-width="2" stroke-linecap="round" />
          </g>
        `}).join("")}
    </g>
  `,Te=L.value.map(l=>{const d=Se(l,t);if(!d)return"";const c=Xe(l),u=d.x,h=d.y,y=d.width,N=d.height,x=Le(N),qe=ke(l),Ie=10,Ce=u+3,Fe=h+32,_e=u+y-ce-x.width,Ne=h+ce,Qe=h+52,Je=h+72,et=Math.max(1,Math.min(4,Math.floor((N-70)/14))),tt=Math.max(4,Math.floor((y-28-x.reserve)/12)),rt=Math.max(8,Math.floor((y-28)/10)),at=Math.max(8,Math.floor((y-28)/12)),De=Be(l.studentNames,at,et).map((ot,it)=>{const st=it===0?0:14;return`<tspan x="${u+14}" dy="${st}">${g(ot)}</tspan>`}).join("");return`
      <g filter="url(#cardShadow)">
        <rect x="${u}" y="${h}" width="${y}" height="${N}" rx="20" fill="${c.fill}" stroke="${c.stroke}" />
        <line x1="${Ce}" y1="${h+Ie}" x2="${Ce}" y2="${h+N-Ie}" stroke="${c.accent}" stroke-width="5" stroke-linecap="round" />
        <text x="${u+14}" y="${Fe}" font-size="15" font-weight="700" fill="#0f172a">${g(q(l.subject,tt))}</text>
        <rect x="${_e}" y="${Ne}" width="${x.width}" height="${x.height}" rx="${x.height/2}" fill="${c.badge}" />
        <text x="${_e+x.width/2}" y="${Ne+x.height/2+x.fontSize*.35}" text-anchor="middle" font-size="${x.fontSize}" font-weight="700" fill="${c.badgeText}">${g(qe)}</text>
        <text x="${u+14}" y="${Qe}" font-size="11" font-weight="600" fill="#64748b">${g(q(l.timeRange,rt))}</text>
        ${De?`<text x="${u+14}" y="${Je}" font-size="12" fill="#475569">${De}</text>`:""}
      </g>
    `}).join(""),R=L.value.length?"":`
      <g>
        <rect x="${m+120}" y="${f+110}" width="${I-240}" height="220" rx="36" fill="rgba(255,255,255,0.84)" stroke="#dbe7f3" />
        <text x="${e/2}" y="${f+206}" text-anchor="middle" font-size="34" font-weight="700" fill="#0f172a">${g(`${ye.value}暂无课程安排`)}</text>
        <text x="${e/2}" y="${f+246}" text-anchor="middle" font-size="16" fill="#64748b">如需新增课程，请前往排课管理。</text>
      </g>
    `,j=`
    <g opacity="0.96">
      <g transform="translate(-54 ${a-134})" opacity="0.78">
        <path d="M 0 96 A 96 96 0 0 1 192 96" fill="none" stroke="#fb7185" stroke-width="16" stroke-linecap="round" />
        <path d="M 16 96 A 80 80 0 0 1 176 96" fill="none" stroke="#f59e0b" stroke-width="16" stroke-linecap="round" />
        <path d="M 32 96 A 64 64 0 0 1 160 96" fill="none" stroke="#facc15" stroke-width="16" stroke-linecap="round" />
        <path d="M 48 96 A 48 48 0 0 1 144 96" fill="none" stroke="#4ade80" stroke-width="16" stroke-linecap="round" />
        <path d="M 64 96 A 32 32 0 0 1 128 96" fill="none" stroke="#60a5fa" stroke-width="16" stroke-linecap="round" />
        <circle cx="22" cy="98" r="18" fill="rgba(255,255,255,0.95)" />
        <circle cx="42" cy="88" r="22" fill="rgba(255,255,255,0.96)" />
        <circle cx="68" cy="98" r="16" fill="rgba(255,255,255,0.94)" />
        <circle cx="120" cy="98" r="16" fill="rgba(255,255,255,0.94)" />
        <circle cx="148" cy="86" r="24" fill="rgba(255,255,255,0.96)" />
        <circle cx="176" cy="98" r="18" fill="rgba(255,255,255,0.95)" />
      </g>
      <g transform="translate(${e-650} 22)">
        <path d="M 48 110 C 44 136 44 154 34 182" fill="none" stroke="rgba(148,163,184,0.58)" stroke-width="2.5" stroke-linecap="round" />
        <path d="M 102 96 C 100 126 104 144 96 178" fill="none" stroke="rgba(148,163,184,0.58)" stroke-width="2.5" stroke-linecap="round" />
        <path d="M 154 118 C 152 144 156 162 150 190" fill="none" stroke="rgba(148,163,184,0.58)" stroke-width="2.5" stroke-linecap="round" />
        <ellipse cx="48" cy="70" rx="28" ry="36" fill="rgba(251,113,133,0.84)" stroke="rgba(225,29,72,0.22)" />
        <ellipse cx="102" cy="56" rx="30" ry="38" fill="rgba(96,165,250,0.84)" stroke="rgba(37,99,235,0.22)" />
        <ellipse cx="154" cy="82" rx="26" ry="34" fill="rgba(253,224,71,0.88)" stroke="rgba(217,119,6,0.22)" />
        <path d="M 48 104 L 42 116 L 54 116 Z" fill="rgba(251,113,133,0.90)" />
        <path d="M 102 92 L 96 104 L 108 104 Z" fill="rgba(96,165,250,0.90)" />
        <path d="M 154 114 L 148 126 L 160 126 Z" fill="rgba(253,224,71,0.92)" />
        <circle cx="40" cy="56" r="6" fill="rgba(255,255,255,0.24)" />
        <circle cx="92" cy="42" r="7" fill="rgba(255,255,255,0.24)" />
        <circle cx="146" cy="68" r="6" fill="rgba(255,255,255,0.22)" />
      </g>
      <g transform="translate(${e-408} 10)">
        <circle cx="70" cy="56" r="48" fill="rgba(253,224,71,0.20)" filter="url(#softBlur)" />
        <circle cx="70" cy="56" r="34" fill="#fde047" stroke="rgba(245,158,11,0.34)" />
        <path d="M 70 0 V 18" stroke="rgba(245,158,11,0.48)" stroke-width="6" stroke-linecap="round" />
        <path d="M 70 94 V 112" stroke="rgba(245,158,11,0.48)" stroke-width="6" stroke-linecap="round" />
        <path d="M 14 56 H 32" stroke="rgba(245,158,11,0.48)" stroke-width="6" stroke-linecap="round" />
        <path d="M 108 56 H 126" stroke="rgba(245,158,11,0.48)" stroke-width="6" stroke-linecap="round" />
        <path d="M 28 16 L 40 28" stroke="rgba(245,158,11,0.48)" stroke-width="6" stroke-linecap="round" />
        <path d="M 100 84 L 112 96" stroke="rgba(245,158,11,0.48)" stroke-width="6" stroke-linecap="round" />
        <path d="M 28 96 L 40 84" stroke="rgba(245,158,11,0.48)" stroke-width="6" stroke-linecap="round" />
        <path d="M 100 28 L 112 16" stroke="rgba(245,158,11,0.48)" stroke-width="6" stroke-linecap="round" />
        <circle cx="58" cy="54" r="4" fill="#334155" />
        <circle cx="82" cy="54" r="4" fill="#334155" />
        <path d="M 56 70 Q 70 82 84 70" fill="none" stroke="#334155" stroke-width="3" stroke-linecap="round" />
        <circle cx="50" cy="66" r="5" fill="rgba(251,113,133,0.26)" />
        <circle cx="90" cy="66" r="5" fill="rgba(251,113,133,0.26)" />
      </g>
      <g transform="translate(${e-320} 36)">
        <ellipse cx="126" cy="58" rx="62" ry="28" fill="rgba(255,255,255,0.92)" stroke="rgba(148,163,184,0.28)" />
        <circle cx="84" cy="58" r="28" fill="rgba(255,255,255,0.96)" stroke="rgba(148,163,184,0.28)" />
        <circle cx="126" cy="42" r="34" fill="rgba(255,255,255,0.96)" stroke="rgba(148,163,184,0.28)" />
        <circle cx="166" cy="58" r="24" fill="rgba(255,255,255,0.96)" stroke="rgba(148,163,184,0.28)" />
        <circle cx="104" cy="70" r="4" fill="#334155" />
        <circle cx="144" cy="70" r="4" fill="#334155" />
        <circle cx="92" cy="82" r="5" fill="rgba(251,113,133,0.38)" />
        <circle cx="156" cy="82" r="5" fill="rgba(251,113,133,0.38)" />
        <path d="M 114 88 Q 124 98 134 88" fill="none" stroke="#334155" stroke-width="3" stroke-linecap="round" />
        <path d="M 58 28 L 64 16 L 70 28 L 82 34 L 70 40 L 64 52 L 58 40 L 46 34 Z" fill="rgba(253,224,71,0.82)" stroke="rgba(245,158,11,0.44)" />
        <path d="M 198 24 L 202 16 L 206 24 L 214 28 L 206 32 L 202 40 L 198 32 L 190 28 Z" fill="rgba(125,211,252,0.88)" stroke="rgba(59,130,246,0.34)" />
        <path d="M 220 76 L 225 66 L 230 76 L 240 81 L 230 86 L 225 96 L 220 86 L 210 81 Z" fill="rgba(253,224,71,0.72)" stroke="rgba(245,158,11,0.34)" />
      </g>
      <g transform="translate(180 ${a-208}) rotate(-10)" opacity="0.84">
        <rect x="0" y="18" width="34" height="172" rx="16" fill="#fbbf24" stroke="rgba(180,83,9,0.24)" />
        <rect x="7" y="32" width="20" height="90" rx="10" fill="rgba(255,255,255,0.28)" />
        <rect x="0" y="128" width="34" height="22" rx="8" fill="#fb7185" />
        <path d="M 0 18 L 17 0 L 34 18 Z" fill="#fde68a" stroke="rgba(148,163,184,0.24)" />
        <path d="M 11 8 L 17 2 L 23 8 Z" fill="#475569" />
      </g>
      <g opacity="0.72">
        <path d="M ${r+190} 54 L ${r+196} 42 L ${r+202} 54 L ${r+214} 60 L ${r+202} 66 L ${r+196} 78 L ${r+190} 66 L ${r+178} 60 Z" fill="rgba(253,224,71,0.82)" />
        <path d="M ${r+226} 114 L ${r+230} 106 L ${r+234} 114 L ${r+242} 118 L ${r+234} 122 L ${r+230} 130 L ${r+226} 122 L ${r+218} 118 Z" fill="rgba(125,211,252,0.88)" />
      </g>
    </g>
  `,K=`
    <svg xmlns="http://www.w3.org/2000/svg" width="${e}" height="${a}" viewBox="0 0 ${e} ${a}">
      <defs>
        <linearGradient id="posterBg" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#f8fbff" />
          <stop offset="100%" stop-color="#eef6ff" />
        </linearGradient>
        <radialGradient id="posterGlowTop" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#7dd3fc" stop-opacity="0.48" />
          <stop offset="100%" stop-color="#7dd3fc" stop-opacity="0" />
        </radialGradient>
        <radialGradient id="posterGlowBottom" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#bfdbfe" stop-opacity="0.56" />
          <stop offset="100%" stop-color="#bfdbfe" stop-opacity="0" />
        </radialGradient>
        <linearGradient id="headerGlass" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#ffffff" stop-opacity="0.88" />
          <stop offset="100%" stop-color="#ffffff" stop-opacity="0.52" />
        </linearGradient>
        <linearGradient id="boardGlass" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#ffffff" stop-opacity="0.60" />
          <stop offset="100%" stop-color="#ffffff" stop-opacity="0.34" />
        </linearGradient>
        <linearGradient id="titleInk" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stop-color="#0f172a" />
          <stop offset="55%" stop-color="#1e3a8a" />
          <stop offset="100%" stop-color="#2563eb" />
        </linearGradient>
        <pattern id="posterDots" width="28" height="28" patternUnits="userSpaceOnUse">
          <circle cx="4" cy="4" r="1.8" fill="rgba(191,219,254,0.58)" />
          <circle cx="18" cy="16" r="1.2" fill="rgba(125,211,252,0.42)" />
        </pattern>
        <filter id="softBlur" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="18" />
        </filter>
        <filter id="titleShadow" x="-20%" y="-40%" width="160%" height="220%">
          <feDropShadow dx="0" dy="5" stdDeviation="6" flood-color="#1d4ed8" flood-opacity="0.14" />
        </filter>
        <filter id="cardShadow" x="-20%" y="-20%" width="160%" height="180%">
          <feDropShadow dx="0" dy="8" stdDeviation="10" flood-color="#0f172a" flood-opacity="0.10" />
        </filter>
      </defs>
      <rect width="${e}" height="${a}" rx="40" fill="url(#posterBg)" />
      <rect width="${e}" height="${a}" rx="40" fill="url(#posterDots)" opacity="0.42" />
      <ellipse cx="${e-150}" cy="92" rx="230" ry="150" fill="url(#posterGlowTop)" filter="url(#softBlur)" />
      <ellipse cx="120" cy="${a-110}" rx="250" ry="180" fill="url(#posterGlowBottom)" filter="url(#softBlur)" />
      <path d="M 0 ${a-168} C 240 ${a-236}, 460 ${a-80}, 760 ${a-138} S 1290 ${a-230}, ${e} ${a-124} L ${e} ${a} L 0 ${a} Z" fill="rgba(255,255,255,0.26)" />
      <path d="M ${e-410} 54 C ${e-330} 18, ${e-232} 20, ${e-164} 82" fill="none" stroke="rgba(255,255,255,0.60)" stroke-width="3" stroke-linecap="round" />
      <path d="M 110 102 C 190 54, 302 54, 386 98" fill="none" stroke="rgba(59,130,246,0.16)" stroke-width="4" stroke-linecap="round" />
      <rect x="${r}" y="${r}" width="${ae}" height="${E}" rx="34" fill="url(#headerGlass)" stroke="rgba(255,255,255,0.88)" />
      <rect x="${r+14}" y="${r+14}" width="${ae-28}" height="${E-28}" rx="28" fill="rgba(255,255,255,0.12)" stroke="rgba(191,219,254,0.38)" />
      <rect x="${r}" y="${B}" width="${o-18}" height="${G}" rx="30" fill="url(#boardGlass)" stroke="rgba(203,213,225,0.82)" />
      <rect x="${r+10}" y="${B+14}" width="${o-38}" height="${G-28}" rx="24" fill="rgba(255,255,255,0.30)" stroke="rgba(255,255,255,0.42)" />
      <rect x="${m-18}" y="${B}" width="${I+36}" height="${G}" rx="34" fill="url(#boardGlass)" stroke="rgba(191,219,254,0.82)" />
      <circle cx="${e-96}" cy="90" r="46" fill="rgba(255,255,255,0.32)" />
      <circle cx="${e-152}" cy="136" r="18" fill="rgba(59,130,246,0.16)" />
      ${j}
      <text x="${C}" y="${r+38}" font-size="19" fill="#2563eb" font-weight="800" letter-spacing="2.8" font-family="'Avenir Next', 'Helvetica Neue', Arial, sans-serif">QINGQINGKETANG</text>
        <text x="${C}" y="${r+88}" font-size="40" fill="url(#titleInk)" font-weight="800" letter-spacing="1.1" font-family="'PingFang SC', 'Hiragino Sans GB', 'Noto Sans SC', sans-serif" filter="url(#titleShadow)">${g(xe.value)}</text>
        <text x="${C}" y="${r+120}" font-size="17" fill="#475569" font-weight="700" letter-spacing="0.9" font-family="'Avenir Next', 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif">${g($e.value)}</text>
      ${de}
      ${fe}
      ${he}
      ${ue}
      ${Te}
      ${R}
    </svg>
  `;return Pe(K)});return lt(async()=>{be(),typeof ResizeObserver<"u"&&Z.value&&(H=new ResizeObserver(()=>{be()}),H.observe(Z.value)),await F()}),ct(()=>{H==null||H.disconnect()}),(t,e)=>{const a=U("el-alert"),r=U("el-button"),o=U("el-image"),i=U("el-empty"),s=U("el-card");return k(),D("section",xt,[V(s,{shadow:"never",class:"schedule-board-card"},{default:O(()=>[v.value?(k(),D("div",kt,"课表加载中…")):z.value?(k(),ge(a,{key:1,title:z.value,type:"error","show-icon":"",closable:!1,class:"schedule-board-alert"},null,8,["title"])):(k(),D("div",wt,[ze("div",{ref_key:"schedulePosterFrameRef",ref:Z,class:"schedule-poster-frame"},[ze("div",vt,[V(r,{class:"schedule-toolbar-button",disabled:v.value||!!$.value,onClick:e[0]||(e[0]=n=>we(-1))},{default:O(()=>[...e[2]||(e[2]=[ne(" 上一周 ",-1)])]),_:1},8,["disabled"]),V(r,{class:"schedule-toolbar-button",disabled:v.value||!!$.value,onClick:Ue},{default:O(()=>[...e[3]||(e[3]=[ne(" 回到本周 ",-1)])]),_:1},8,["disabled"]),V(r,{class:"schedule-toolbar-button",disabled:v.value||!!$.value,onClick:e[1]||(e[1]=n=>we(1))},{default:O(()=>[...e[4]||(e[4]=[ne(" 下一周 ",-1)])]),_:1},8,["disabled"]),V(r,{class:"schedule-toolbar-button",disabled:v.value||!!$.value||!!z.value||!T.value,onClick:Re},{default:O(()=>[...e[5]||(e[5]=[ne(" 打印课表 ",-1)])]),_:1},8,["disabled"])]),T.value?(k(),ge(o,{key:0,src:T.value,"preview-src-list":[T.value],class:"schedule-poster-image",fit:"contain","preview-teleported":"","hide-on-click-modal":""},null,8,["src","preview-src-list"])):dt("",!0),Me.value.length?(k(),D("div",{key:1,class:"schedule-poster-action-layer",style:Ae(Ye.value)},[(k(!0),D(ft,null,ut(Me.value,n=>(k(),D("button",{key:n.groupKey,type:"button",class:"schedule-session-action","data-state":n.state,"data-size":n.size,disabled:!!$.value,style:Ae(n.style),"aria-label":n.ariaLabel,onClick:ht(J=>Ze(n),["stop"])},gt($.value===n.groupKey?"处理中":n.label),13,Lt))),128))],4)):(k(),ge(i,{key:2,description:"该周暂无排课安排","image-size":72}))],512)]))]),_:1})])}}},Nt=nt(Mt,[["__scopeId","data-v-36d2fbda"]]);export{Nt as default};
