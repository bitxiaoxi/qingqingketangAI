import{_ as Ot,k as R,i as rt,o as Ut,b as $,c as I,d as z,f as K,g as ct,e as Lt,u as at,s as Xt,F as qt,r as Ft,h as ot,n as St,l as V,j as y,H as Yt,q as Qt,t as Jt,E as Mt,I as te,J as ee,K as re,L as ae,C as oe}from"./index-BkgvB4Rl.js";import{a as dt}from"./api-Bm0ghaWz.js";const ie={class:"page-stack"},se={key:0,class:"page-state schedule-board-state"},le={key:2,class:"schedule-poster-section"},ne={class:"schedule-poster-frame"},ce={class:"schedule-poster-toolbar"},de={key:1,class:"schedule-poster-action-layer"},fe=["data-state","disabled","aria-label","onClick"],ue=7*24*60*60*1e3,he={__name:"CoursesView",setup(ge){const k=R(!1),_=R(""),N=R([]),g=R(rt(new Date)),b=R("");let Z=0;const p=e=>String(e??"").replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;").replace(/'/g,"&#39;"),Tt=(e,t)=>{const r=String(e??"");return r.length<=t?r:`${r.slice(0,Math.max(0,t-1))}…`},O=e=>Array.from(String(e??"")).reduce((t,r)=>t+(/[\u0000-\u00ff]/.test(r)?.55:1),0),ft=(e,t)=>{const r=String(e??"");if(O(r)<=t)return r;let a="";for(const o of Array.from(r)){const i=`${a}${o}`;if(O(`${i}…`)>t)break;a=i}return`${a}…`},Ct=(e,t,r)=>{const a=(e??[]).map(s=>String(s??"").trim()).filter(Boolean);if(!a.length||t<=0||r<=0)return[];const o=[];let i=0;for(;i<a.length&&o.length<r;){let s="";for(;i<a.length;){const l=s?`、${a[i]}`:a[i];if(O(`${s}${l}`)<=t){s+=l,i+=1;continue}s||(s=ft(a[i],t),i+=1);break}if(!s)break;o.push(s)}if(i<a.length&&o.length){const s=`等${a.length}位`,l=Math.max(3,t-O(s));o[o.length-1]=`${ft(o[o.length-1],l)}${s}`}return o},D=e=>{const t=new Date(e);return t.getHours()*60+t.getMinutes()},It=e=>{const t=Math.floor(e/60);return`${String(t).padStart(2,"0")}:00`},_t=e=>`data:image/svg+xml;charset=utf-8,${encodeURIComponent(e)}`,Nt=()=>{if(!L.value||typeof window>"u"||typeof document>"u")return;const e=document.createElement("iframe");e.setAttribute("aria-hidden","true"),e.style.position="fixed",e.style.width="0",e.style.height="0",e.style.border="0",e.style.right="0",e.style.bottom="0",document.body.appendChild(e);const t=()=>{window.setTimeout(()=>{document.body.contains(e)&&document.body.removeChild(e)},300)},r=e.contentWindow;if(!r){t();return}const a=p(`${gt.value} ${ut.value}`);r.document.open(),r.document.write(`
    <!doctype html>
    <html lang="zh-CN">
      <head>
        <meta charset="UTF-8" />
        <title>${a}</title>
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
          <img id="poster" src="${L.value}" alt="${a}" />
        </div>
      </body>
    </html>
  `),r.document.close();const o=r.document.getElementById("poster"),i=()=>{r.focus(),r.print()};if(r.onafterprint=t,o!=null&&o.complete){window.setTimeout(i,120);return}o==null||o.addEventListener("load",()=>{window.setTimeout(i,120)},{once:!0}),o==null||o.addEventListener("error",t,{once:!0})},U=async(e=g.value)=>{const t=++Z;k.value=!0,_.value="",N.value=[];try{const r=await dt.listWeekSchedules(ot(e));if(t!==Z)return;N.value=r??[]}catch(r){if(t!==Z)return;_.value=St(r,"课表加载失败")}finally{t===Z&&(k.value=!1)}},Dt=(e,t)=>{const r=new Set((e??[]).map(a=>String(a)));N.value=(N.value??[]).map(a=>r.has(String(a.id))?{...a,status:t}:a)},Ht=y(()=>(N.value??[]).slice().sort((e,t)=>new Date(e.startTime)-new Date(t.startTime)).map(e=>{const t=String(e.status??"").toUpperCase()==="COMPLETED";return{...e,scheduleId:e.id,studentName:e.studentName??"未命名",subject:e.subject??"正式课",timeRange:oe(e.startTime,e.endTime),isCompleted:t}})),w=y(()=>{const e=new Map;return Ht.value.forEach(t=>{const r=ot(t.startTime),a=[r,t.startTime,t.endTime,t.subject].join("|"),o=e.get(a);if(o){o.scheduleIds.push(t.scheduleId),o.studentIds.add(t.studentId??t.scheduleId),o.studentNames.add(t.studentName),t.isCompleted?o.completedScheduleIds.push(t.scheduleId):o.pendingScheduleIds.push(t.scheduleId);return}e.set(a,{groupKey:a,dateKey:r,startTime:t.startTime,endTime:t.endTime,sortTime:new Date(t.startTime).getTime(),timeRange:t.timeRange,subject:t.subject,scheduleIds:[t.scheduleId],completedScheduleIds:t.isCompleted?[t.scheduleId]:[],pendingScheduleIds:t.isCompleted?[]:[t.scheduleId],studentIds:new Set([t.studentId??t.scheduleId]),studentNames:new Set([t.studentName])})}),Array.from(e.values()).map(t=>{const r=Array.from(t.studentNames),a=t.studentIds.size||t.scheduleIds.length,o=t.completedScheduleIds.length,i=t.pendingScheduleIds.length,s=o===t.scheduleIds.length,l=o>0&&i>0;return{groupKey:t.groupKey,dateKey:t.dateKey,startTime:t.startTime,endTime:t.endTime,sortTime:t.sortTime,timeRange:t.timeRange,subject:t.subject,scheduleIds:t.scheduleIds,completedScheduleIds:t.completedScheduleIds,pendingScheduleIds:t.pendingScheduleIds,participantCount:a,participantLabel:`共 ${a} 位学员`,studentNames:r,studentNamesLabel:r.join("、"),completedCount:o,pendingCount:i,isCompleted:s,isPartial:l,sessionNote:a>1?"小组课":"单人课"}}).sort((t,r)=>t.sortTime-r.sortTime)}),X=y(()=>{const e=ot(new Date);return ee(g.value).map(t=>{const r=ot(t),a=t.getDay(),o=w.value.filter(i=>i.dateKey===r);return{dateKey:r,weekday:ae(t),dateLabel:re(t),dateNumber:`${t.getDate()}`.padStart(2,"0"),monthLabel:`${t.getMonth()+1}月`,isToday:r===e,isWeekend:a===0||a===6,items:o}})}),ut=y(()=>te(g.value)),Gt=y(()=>{const e=g.value?g.value.getTime():0,t=rt(new Date).getTime();return Math.round((e-t)/ue)}),ht=y(()=>{const e=Gt.value;return e===0?"本周":e===-1?"上周":e===1?"下周":e>0?`${e} 周后`:`${Math.abs(e)} 周前`}),gt=y(()=>`${ht.value}课程表`),Wt=e=>e.isCompleted?"已销课":"待销课",At=e=>e.isCompleted?`取消 ${e.timeRange} ${e.subject} 的销课`:e.isPartial?`继续为 ${e.timeRange} ${e.subject} 销课`:`为 ${e.timeRange} ${e.subject} 销课`,pt=async e=>{const t=new Date(g.value);t.setDate(t.getDate()+e*7),g.value=rt(t),await U(g.value)},Bt=async()=>{g.value=rt(new Date),await U(g.value)},Et=async e=>{if(!e||b.value)return;const t=!e.isCompleted,r=t?e.pendingScheduleIds:e.completedScheduleIds;if(r.length){b.value=e.groupKey;try{for(const a of r)t?await dt.completeSchedule(a):await dt.undoCompleteSchedule(a);Dt(r,t?"COMPLETED":"PLANNED"),Mt.success(t?"已销课":"已恢复为待上课")}catch(a){Mt.error(St(a,t?"销课失败":"恢复待上课失败")),await U(g.value)}finally{b.value=""}}},mt=()=>{if(!X.value.length)return null;const e=1600,t=36,r=122,a=12,o=Math.floor((e-t*2-r-a*6)/7),i=162,s=78,l=38,q=8*60,F=22*60;let x=q,f=F;if(w.value.length){const B=Math.min(...w.value.map(j=>D(j.startTime))),E=Math.max(...w.value.map(j=>D(j.endTime)));x=Math.max(7*60,Math.floor(B/60)*60-60),f=Math.min(23*60,Math.ceil(E/60)*60+60),f-x<8*60&&(f=Math.min(23*60,x+8*60))}const Y=Math.max(1,Math.ceil((f-x)/60)),Q=84,m=t+i+s,v=Y*Q,S=m+v,J=S+l,H=t+r,M=t+i,G=new Map(X.value.map((B,E)=>[B.dateKey,E])),W=7*o+6*a,T=e-t*2,tt=i-24,et=t+24,A=M-16,it=S-A,st=H-24,lt=t+12,nt=r-42;return{width:e,height:J,outerPadding:t,leftRail:r,columnGap:a,columnWidth:o,headerHeight:i,dayHeaderHeight:s,bottomPadding:l,visibleStart:x,visibleEnd:f,hourRows:Y,hourHeight:Q,gridTop:m,gridHeight:v,gridBottom:S,gridLeft:H,dayHeaderTop:M,dayIndexMap:G,gridAreaWidth:W,titlePanelWidth:T,titlePanelHeight:tt,titleTextX:et,boardPanelY:A,boardPanelHeight:it,timelineAxisX:st,timelineLabelX:lt,timelineLabelWidth:nt,timelineLabelHeight:28}},jt=e=>e.isCompleted?{fill:"#f0fdf4",stroke:"#86efac",accent:"#22c55e",badge:"#dcfce7",badgeText:"#15803d"}:e.isPartial?{fill:"#eff6ff",stroke:"#93c5fd",accent:"#3b82f6",badge:"#dbeafe",badgeText:"#1d4ed8"}:{fill:"#fff7ed",stroke:"#fdba74",accent:"#f59e0b",badge:"#ffedd5",badgeText:"#c2410c"},bt=(e,t)=>{const r=t.dayIndexMap.get(e.dateKey);if(r===void 0)return null;const a=t.gridLeft+r*(t.columnWidth+t.columnGap)+10,o=t.gridTop+(D(e.startTime)-t.visibleStart)/60*t.hourHeight+8,i=t.columnWidth-20,s=Math.max(76,(D(e.endTime)-D(e.startTime))/60*t.hourHeight-12);return{x:a,y:o,width:i,height:s,compact:s<102}},$t=y(()=>{const e=mt();return e?w.value.map(t=>{const r=bt(t,e);return r?{...t,label:Wt(t),ariaLabel:At(t),state:t.isCompleted?"completed":t.isPartial?"partial":"pending",style:{top:`${(r.y+(r.compact?r.height/2:r.height-26))/e.height*100}%`,left:`${(r.x+14)/e.width*100}%`}}:null}).filter(Boolean):[]}),L=y(()=>{const e=mt();if(!e)return"";const{width:t,height:r,outerPadding:a,leftRail:o,columnGap:i,columnWidth:s,dayHeaderHeight:l,visibleStart:q,hourRows:F,hourHeight:x,gridTop:f,gridHeight:Y,gridBottom:Q,gridLeft:m,dayHeaderTop:v,gridAreaWidth:S,titlePanelWidth:J,titlePanelHeight:H,titleTextX:M,boardPanelY:G,boardPanelHeight:W,timelineAxisX:T,timelineLabelX:tt,timelineLabelWidth:et,timelineLabelHeight:A}=e,it=X.value.map((n,c)=>{const d=m+c*(s+i),u=n.isToday?"#dbeafe":n.isWeekend?"#f0f9ff":"#ffffff",h=n.isToday?"#60a5fa":"#dbe7f3",C=n.items.length?`${n.items.length} 节课`:"空档";return`
      <g>
        <rect x="${d}" y="${v}" width="${s}" height="${l-12}" rx="22" fill="${u}" stroke="${h}" />
        <text x="${d+18}" y="${v+30}" font-size="16" font-weight="700" fill="#0f172a">${p(n.weekday)}</text>
        <text x="${d+18}" y="${v+54}" font-size="12" fill="#64748b">${p(`${n.monthLabel} ${n.dateNumber}`)}</text>
        <text x="${d+s-18}" y="${v+54}" text-anchor="end" font-size="12" fill="${n.isToday?"#2563eb":"#94a3b8"}">${p(n.isToday?"今天":C)}</text>
      </g>
    `}).join(""),st=X.value.map((n,c)=>`<rect x="${m+c*(s+i)}" y="${f}" width="${s}" height="${Y}" rx="26" fill="${n.isWeekend?"#f8fcff":"#ffffff"}" stroke="#e5edf5" />`).join(""),lt=Array.from({length:F+1},(n,c)=>{const d=f+c*x;return`
      <line x1="${m}" y1="${d}" x2="${m+7*s+6*i}" y2="${d}" stroke="#e8eef5" />
    `}).join(""),nt=`
    <g>
      <line x1="${T}" y1="${f+14}" x2="${T}" y2="${Q-14}" stroke="rgba(148,163,184,0.38)" stroke-width="3" stroke-linecap="round" />
      ${Array.from({length:F},(n,c)=>{const u=f+c*x+8,h=u+A/2,C=T+10,P=m-8;return`
          <g>
            <rect x="${tt}" y="${u}" width="${et}" height="${A}" rx="14" fill="rgba(255,255,255,0.92)" stroke="rgba(191,219,254,0.92)" />
            <text x="${tt+et/2}" y="${u+18}" text-anchor="middle" font-size="12" font-weight="700" fill="#475569">${p(It(q+c*60))}</text>
            <circle cx="${T}" cy="${h}" r="5" fill="#ffffff" stroke="#93c5fd" stroke-width="3" />
            <line x1="${C}" y1="${h}" x2="${P}" y2="${h}" stroke="rgba(191,219,254,0.78)" stroke-width="2" stroke-linecap="round" />
          </g>
        `}).join("")}
    </g>
  `,yt=w.value.map(n=>{const c=bt(n,e);if(!c)return"";const d=jt(n),u=c.x,h=c.y,C=c.width,P=c.height,xt=10,kt=u+3,wt=c.compact,Pt=h+80,Rt=Math.max(1,Math.min(4,Math.floor((P-92)/14))),zt=Math.max(8,Math.floor((C-44)/12)),vt=(wt?[]:Ct(n.studentNames,zt,Rt)).map((Kt,Vt)=>{const Zt=Vt===0?0:15;return`<tspan x="${u+14}" dy="${Zt}">${p(Kt)}</tspan>`}).join("");return`
      <g filter="url(#cardShadow)">
        <rect x="${u}" y="${h}" width="${C}" height="${P}" rx="20" fill="${d.fill}" stroke="${d.stroke}" />
        <line x1="${kt}" y1="${h+xt}" x2="${kt}" y2="${h+P-xt}" stroke="${d.accent}" stroke-width="5" stroke-linecap="round" />
        <rect x="${u+14}" y="${h+14}" width="74" height="24" rx="12" fill="${d.badge}" />
        <text x="${u+51}" y="${h+30}" text-anchor="middle" font-size="11" font-weight="600" fill="${d.badgeText}">${p(n.timeRange)}</text>
        <text x="${u+14}" y="${h+58}" font-size="17" font-weight="700" fill="#0f172a">${p(Tt(n.subject,14))}</text>
        ${wt||!vt?"":`<text x="${u+14}" y="${Pt}" font-size="12" fill="#475569">${vt}</text>`}
      </g>
    `}).join(""),B=w.value.length?"":`
      <g>
        <rect x="${m+120}" y="${f+110}" width="${S-240}" height="220" rx="36" fill="rgba(255,255,255,0.84)" stroke="#dbe7f3" />
        <text x="${t/2}" y="${f+206}" text-anchor="middle" font-size="34" font-weight="700" fill="#0f172a">${p(`${ht.value}暂无课程安排`)}</text>
        <text x="${t/2}" y="${f+246}" text-anchor="middle" font-size="16" fill="#64748b">如需新增课程，请前往排课管理。</text>
      </g>
    `,E=`
    <g opacity="0.96">
      <g transform="translate(-54 ${r-134})" opacity="0.78">
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
      <g transform="translate(${t-650} 22)">
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
      <g transform="translate(${t-408} 10)">
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
      <g transform="translate(${t-320} 36)">
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
      <g transform="translate(180 ${r-208}) rotate(-10)" opacity="0.84">
        <rect x="0" y="18" width="34" height="172" rx="16" fill="#fbbf24" stroke="rgba(180,83,9,0.24)" />
        <rect x="7" y="32" width="20" height="90" rx="10" fill="rgba(255,255,255,0.28)" />
        <rect x="0" y="128" width="34" height="22" rx="8" fill="#fb7185" />
        <path d="M 0 18 L 17 0 L 34 18 Z" fill="#fde68a" stroke="rgba(148,163,184,0.24)" />
        <path d="M 11 8 L 17 2 L 23 8 Z" fill="#475569" />
      </g>
      <g opacity="0.72">
        <path d="M ${a+190} 54 L ${a+196} 42 L ${a+202} 54 L ${a+214} 60 L ${a+202} 66 L ${a+196} 78 L ${a+190} 66 L ${a+178} 60 Z" fill="rgba(253,224,71,0.82)" />
        <path d="M ${a+226} 114 L ${a+230} 106 L ${a+234} 114 L ${a+242} 118 L ${a+234} 122 L ${a+230} 130 L ${a+226} 122 L ${a+218} 118 Z" fill="rgba(125,211,252,0.88)" />
      </g>
    </g>
  `,j=`
    <svg xmlns="http://www.w3.org/2000/svg" width="${t}" height="${r}" viewBox="0 0 ${t} ${r}">
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
      <rect width="${t}" height="${r}" rx="40" fill="url(#posterBg)" />
      <rect width="${t}" height="${r}" rx="40" fill="url(#posterDots)" opacity="0.42" />
      <ellipse cx="${t-150}" cy="92" rx="230" ry="150" fill="url(#posterGlowTop)" filter="url(#softBlur)" />
      <ellipse cx="120" cy="${r-110}" rx="250" ry="180" fill="url(#posterGlowBottom)" filter="url(#softBlur)" />
      <path d="M 0 ${r-168} C 240 ${r-236}, 460 ${r-80}, 760 ${r-138} S 1290 ${r-230}, ${t} ${r-124} L ${t} ${r} L 0 ${r} Z" fill="rgba(255,255,255,0.26)" />
      <path d="M ${t-410} 54 C ${t-330} 18, ${t-232} 20, ${t-164} 82" fill="none" stroke="rgba(255,255,255,0.60)" stroke-width="3" stroke-linecap="round" />
      <path d="M 110 102 C 190 54, 302 54, 386 98" fill="none" stroke="rgba(59,130,246,0.16)" stroke-width="4" stroke-linecap="round" />
      <rect x="${a}" y="${a}" width="${J}" height="${H}" rx="34" fill="url(#headerGlass)" stroke="rgba(255,255,255,0.88)" />
      <rect x="${a+14}" y="${a+14}" width="${J-28}" height="${H-28}" rx="28" fill="rgba(255,255,255,0.12)" stroke="rgba(191,219,254,0.38)" />
      <rect x="${a}" y="${G}" width="${o-18}" height="${W}" rx="30" fill="url(#boardGlass)" stroke="rgba(203,213,225,0.82)" />
      <rect x="${a+10}" y="${G+14}" width="${o-38}" height="${W-28}" rx="24" fill="rgba(255,255,255,0.30)" stroke="rgba(255,255,255,0.42)" />
      <rect x="${m-18}" y="${G}" width="${S+36}" height="${W}" rx="34" fill="url(#boardGlass)" stroke="rgba(191,219,254,0.82)" />
      <circle cx="${t-96}" cy="90" r="46" fill="rgba(255,255,255,0.32)" />
      <circle cx="${t-152}" cy="136" r="18" fill="rgba(59,130,246,0.16)" />
      ${E}
      <text x="${M}" y="${a+38}" font-size="19" fill="#2563eb" font-weight="800" letter-spacing="2.8" font-family="'Avenir Next', 'Helvetica Neue', Arial, sans-serif">QINGQINGKETANG</text>
        <text x="${M}" y="${a+88}" font-size="40" fill="url(#titleInk)" font-weight="800" letter-spacing="1.1" font-family="'PingFang SC', 'Hiragino Sans GB', 'Noto Sans SC', sans-serif" filter="url(#titleShadow)">${p(gt.value)}</text>
        <text x="${M}" y="${a+120}" font-size="17" fill="#475569" font-weight="700" letter-spacing="0.9" font-family="'Avenir Next', 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif">${p(ut.value)}</text>
      ${it}
      ${st}
      ${nt}
      ${lt}
      ${yt}
      ${B}
    </svg>
  `;return _t(j)});return Ut(async()=>{await U()}),(e,t)=>{const r=V("el-alert"),a=V("el-button"),o=V("el-image"),i=V("el-empty"),s=V("el-card");return $(),I("section",ie,[z(s,{shadow:"never",class:"schedule-board-card"},{default:K(()=>[k.value?($(),I("div",se,"课表加载中…")):_.value?($(),ct(r,{key:1,title:_.value,type:"error","show-icon":"",closable:!1,class:"schedule-board-alert"},null,8,["title"])):($(),I("div",le,[Lt("div",ne,[Lt("div",ce,[z(a,{class:"schedule-toolbar-button",disabled:k.value||!!b.value,onClick:t[0]||(t[0]=l=>pt(-1))},{default:K(()=>[...t[2]||(t[2]=[at(" 上一周 ",-1)])]),_:1},8,["disabled"]),z(a,{class:"schedule-toolbar-button",disabled:k.value||!!b.value,onClick:Bt},{default:K(()=>[...t[3]||(t[3]=[at(" 回到本周 ",-1)])]),_:1},8,["disabled"]),z(a,{class:"schedule-toolbar-button",disabled:k.value||!!b.value,onClick:t[1]||(t[1]=l=>pt(1))},{default:K(()=>[...t[4]||(t[4]=[at(" 下一周 ",-1)])]),_:1},8,["disabled"]),z(a,{class:"schedule-toolbar-button",disabled:k.value||!!b.value||!!_.value||!L.value,onClick:Nt},{default:K(()=>[...t[5]||(t[5]=[at(" 打印课表 ",-1)])]),_:1},8,["disabled"])]),L.value?($(),ct(o,{key:0,src:L.value,"preview-src-list":[L.value],class:"schedule-poster-image",fit:"contain","preview-teleported":"","hide-on-click-modal":""},null,8,["src","preview-src-list"])):Xt("",!0),$t.value.length?($(),I("div",de,[($(!0),I(qt,null,Ft($t.value,l=>($(),I("button",{key:l.groupKey,type:"button",class:"schedule-session-action","data-state":l.state,disabled:!!b.value,style:Qt(l.style),"aria-label":l.ariaLabel,onClick:Yt(q=>Et(l),["stop"])},Jt(b.value===l.groupKey?"处理中":l.label),13,fe))),128))])):($(),ct(i,{key:2,description:"该周暂无排课安排","image-size":72}))])]))]),_:1})])}}},$e=Ot(he,[["__scopeId","data-v-9047dd10"]]);export{$e as default};
