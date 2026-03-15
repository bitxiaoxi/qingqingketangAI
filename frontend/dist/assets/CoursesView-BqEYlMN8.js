import{_ as Pe,k as j,i as te,o as Ae,b as y,c as I,d as R,f as z,g as ne,e as ke,s as re,G as Be,F as Ee,r as je,h as ae,n as we,l as K,j as $,H as Re,q as ze,t as Ke,E as ve,I as Ve,J as Ze,K as Oe,L as Ue,u as Xe}from"./index-DFGQWfZ9.js";import{a as ce}from"./api-BJ92196s.js";const qe={class:"page-stack"},Fe={key:0,class:"page-state schedule-board-state"},Qe={key:2,class:"schedule-poster-section"},Ye={class:"schedule-poster-frame"},Je={class:"schedule-poster-toolbar"},et={key:1,class:"schedule-poster-action-layer"},tt=["data-state","disabled","aria-label","onClick"],rt=7*24*60*60*1e3,at={__name:"CoursesView",setup(ot){const k=j(!1),C=j(""),_=j([]),h=j(te(new Date)),b=j("");let V=0;const p=t=>String(t??"").replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;").replace(/'/g,"&#39;"),de=(t,e)=>{const r=String(t??"");return r.length<=e?r:`${r.slice(0,Math.max(0,e-1))}…`},D=t=>{const e=new Date(t);return e.getHours()*60+e.getMinutes()},Le=t=>{const e=Math.floor(t/60);return`${String(e).padStart(2,"0")}:00`},Se=t=>`data:image/svg+xml;charset=utf-8,${encodeURIComponent(t)}`,Me=()=>{if(!L.value||typeof window>"u"||typeof document>"u")return;const t=document.createElement("iframe");t.setAttribute("aria-hidden","true"),t.style.position="fixed",t.style.width="0",t.style.height="0",t.style.border="0",t.style.right="0",t.style.bottom="0",document.body.appendChild(t);const e=()=>{window.setTimeout(()=>{document.body.contains(t)&&document.body.removeChild(t)},300)},r=t.contentWindow;if(!r){e();return}const a=p(`${ge.value} ${fe.value}`);r.document.open(),r.document.write(`
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
  `),r.document.close();const o=r.document.getElementById("poster"),i=()=>{r.focus(),r.print()};if(r.onafterprint=e,o!=null&&o.complete){window.setTimeout(i,120);return}o==null||o.addEventListener("load",()=>{window.setTimeout(i,120)},{once:!0}),o==null||o.addEventListener("error",e,{once:!0})},Z=async(t=h.value)=>{const e=++V;k.value=!0,C.value="",_.value=[];try{const r=await ce.listWeekSchedules(ae(t));if(e!==V)return;_.value=r??[]}catch(r){if(e!==V)return;C.value=we(r,"课表加载失败")}finally{e===V&&(k.value=!1)}},Te=(t,e)=>{const r=new Set((t??[]).map(a=>String(a)));_.value=(_.value??[]).map(a=>r.has(String(a.id))?{...a,status:e}:a)},Ie=$(()=>(_.value??[]).slice().sort((t,e)=>new Date(t.startTime)-new Date(e.startTime)).map(t=>{const e=String(t.status??"").toUpperCase()==="COMPLETED";return{...t,scheduleId:t.id,studentName:t.studentName??"未命名",subject:t.subject??"正式课",timeRange:Xe(t.startTime,t.endTime),isCompleted:e}})),w=$(()=>{const t=new Map;return Ie.value.forEach(e=>{const r=ae(e.startTime),a=[r,e.startTime,e.endTime,e.subject].join("|"),o=t.get(a);if(o){o.scheduleIds.push(e.scheduleId),o.studentIds.add(e.studentId??e.scheduleId),o.studentNames.add(e.studentName),e.isCompleted?o.completedScheduleIds.push(e.scheduleId):o.pendingScheduleIds.push(e.scheduleId);return}t.set(a,{groupKey:a,dateKey:r,startTime:e.startTime,endTime:e.endTime,sortTime:new Date(e.startTime).getTime(),timeRange:e.timeRange,subject:e.subject,scheduleIds:[e.scheduleId],completedScheduleIds:e.isCompleted?[e.scheduleId]:[],pendingScheduleIds:e.isCompleted?[]:[e.scheduleId],studentIds:new Set([e.studentId??e.scheduleId]),studentNames:new Set([e.studentName])})}),Array.from(t.values()).map(e=>{const r=Array.from(e.studentNames),a=e.studentIds.size||e.scheduleIds.length,o=e.completedScheduleIds.length,i=e.pendingScheduleIds.length,s=o===e.scheduleIds.length,n=o>0&&i>0;return{groupKey:e.groupKey,dateKey:e.dateKey,startTime:e.startTime,endTime:e.endTime,sortTime:e.sortTime,timeRange:e.timeRange,subject:e.subject,scheduleIds:e.scheduleIds,completedScheduleIds:e.completedScheduleIds,pendingScheduleIds:e.pendingScheduleIds,participantCount:a,participantLabel:`共 ${a} 位学员`,studentNamesLabel:r.join("、"),completedCount:o,pendingCount:i,isCompleted:s,isPartial:n,sessionNote:a>1?"小组课":"单人课"}}).sort((e,r)=>e.sortTime-r.sortTime)}),O=$(()=>{const t=ae(new Date);return Ze(h.value).map(e=>{const r=ae(e),a=e.getDay(),o=w.value.filter(i=>i.dateKey===r);return{dateKey:r,weekday:Ue(e),dateLabel:Oe(e),dateNumber:`${e.getDate()}`.padStart(2,"0"),monthLabel:`${e.getMonth()+1}月`,isToday:r===t,isWeekend:a===0||a===6,items:o}})}),fe=$(()=>Ve(h.value)),Ce=$(()=>{const t=h.value?h.value.getTime():0,e=te(new Date).getTime();return Math.round((t-e)/rt)}),ue=$(()=>{const t=Ce.value;return t===0?"本周":t===-1?"上周":t===1?"下周":t>0?`${t} 周后`:`${Math.abs(t)} 周前`}),ge=$(()=>`${ue.value}课程表`),_e=t=>t.isCompleted?"已销课":"待销课",De=t=>t.isCompleted?`取消 ${t.timeRange} ${t.subject} 的销课`:t.isPartial?`继续为 ${t.timeRange} ${t.subject} 销课`:`为 ${t.timeRange} ${t.subject} 销课`,he=async t=>{const e=new Date(h.value);e.setDate(e.getDate()+t*7),h.value=te(e),await Z(h.value)},Ge=async()=>{h.value=te(new Date),await Z(h.value)},He=async t=>{if(!t||b.value)return;const e=!t.isCompleted,r=e?t.pendingScheduleIds:t.completedScheduleIds;if(r.length){b.value=t.groupKey;try{for(const a of r)e?await ce.completeSchedule(a):await ce.undoCompleteSchedule(a);Te(r,e?"COMPLETED":"PLANNED"),ve.success(e?"已销课":"已恢复为待上课")}catch(a){ve.error(we(a,e?"销课失败":"恢复待上课失败")),await Z(h.value)}finally{b.value=""}}},pe=()=>{if(!O.value.length)return null;const t=1600,e=36,r=122,a=12,o=Math.floor((t-e*2-r-a*6)/7),i=162,s=78,n=38,U=8*60,X=22*60;let x=U,f=X;if(w.value.length){const P=Math.min(...w.value.map(B=>D(B.startTime))),A=Math.max(...w.value.map(B=>D(B.endTime)));x=Math.max(7*60,Math.floor(P/60)*60-60),f=Math.min(23*60,Math.ceil(A/60)*60+60),f-x<8*60&&(f=Math.min(23*60,x+8*60))}const q=Math.max(1,Math.ceil((f-x)/60)),F=84,m=e+i+s,v=q*F,S=m+v,Q=S+n,G=e+r,M=e+i,H=new Map(O.value.map((P,A)=>[P.dateKey,A])),N=7*o+6*a,T=t-e*2,Y=i-24,J=e+24,W=M-16,oe=S-W,ie=G-24,le=e+12,se=r-42;return{width:t,height:Q,outerPadding:e,leftRail:r,columnGap:a,columnWidth:o,headerHeight:i,dayHeaderHeight:s,bottomPadding:n,visibleStart:x,visibleEnd:f,hourRows:q,hourHeight:F,gridTop:m,gridHeight:v,gridBottom:S,gridLeft:G,dayHeaderTop:M,dayIndexMap:H,gridAreaWidth:N,titlePanelWidth:T,titlePanelHeight:Y,titleTextX:J,boardPanelY:W,boardPanelHeight:oe,timelineAxisX:ie,timelineLabelX:le,timelineLabelWidth:se,timelineLabelHeight:28}},Ne=t=>t.isCompleted?{fill:"#f0fdf4",stroke:"#86efac",accent:"#22c55e",badge:"#dcfce7",badgeText:"#15803d"}:t.isPartial?{fill:"#eff6ff",stroke:"#93c5fd",accent:"#3b82f6",badge:"#dbeafe",badgeText:"#1d4ed8"}:{fill:"#fff7ed",stroke:"#fdba74",accent:"#f59e0b",badge:"#ffedd5",badgeText:"#c2410c"},me=(t,e)=>{const r=e.dayIndexMap.get(t.dateKey);if(r===void 0)return null;const a=e.gridLeft+r*(e.columnWidth+e.columnGap)+10,o=e.gridTop+(D(t.startTime)-e.visibleStart)/60*e.hourHeight+8,i=e.columnWidth-20,s=Math.max(76,(D(t.endTime)-D(t.startTime))/60*e.hourHeight-12);return{x:a,y:o,width:i,height:s,compact:s<102}},be=$(()=>{const t=pe();return t?w.value.map(e=>{const r=me(e,t);return r?{...e,label:_e(e),ariaLabel:De(e),state:e.isCompleted?"completed":e.isPartial?"partial":"pending",style:{top:`${(r.y+(r.compact?r.height/2:r.height-26))/t.height*100}%`,left:`${(r.x+14)/t.width*100}%`}}:null}).filter(Boolean):[]}),L=$(()=>{const t=pe();if(!t)return"";const{width:e,height:r,outerPadding:a,leftRail:o,columnGap:i,columnWidth:s,dayHeaderHeight:n,visibleStart:U,hourRows:X,hourHeight:x,gridTop:f,gridHeight:q,gridBottom:F,gridLeft:m,dayHeaderTop:v,gridAreaWidth:S,titlePanelWidth:Q,titlePanelHeight:G,titleTextX:M,boardPanelY:H,boardPanelHeight:N,timelineAxisX:T,timelineLabelX:Y,timelineLabelWidth:J,timelineLabelHeight:W}=t,oe=O.value.map((l,c)=>{const d=m+c*(s+i),g=l.isToday?"#dbeafe":l.isWeekend?"#f0f9ff":"#ffffff",u=l.isToday?"#60a5fa":"#dbe7f3",E=l.items.length?`${l.items.length} 节课`:"空档";return`
      <g>
        <rect x="${d}" y="${v}" width="${s}" height="${n-12}" rx="22" fill="${g}" stroke="${u}" />
        <text x="${d+18}" y="${v+30}" font-size="16" font-weight="700" fill="#0f172a">${p(l.weekday)}</text>
        <text x="${d+18}" y="${v+54}" font-size="12" fill="#64748b">${p(`${l.monthLabel} ${l.dateNumber}`)}</text>
        <text x="${d+s-18}" y="${v+54}" text-anchor="end" font-size="12" fill="${l.isToday?"#2563eb":"#94a3b8"}">${p(l.isToday?"今天":E)}</text>
      </g>
    `}).join(""),ie=O.value.map((l,c)=>`<rect x="${m+c*(s+i)}" y="${f}" width="${s}" height="${q}" rx="26" fill="${l.isWeekend?"#f8fcff":"#ffffff"}" stroke="#e5edf5" />`).join(""),le=Array.from({length:X+1},(l,c)=>{const d=f+c*x;return`
      <line x1="${m}" y1="${d}" x2="${m+7*s+6*i}" y2="${d}" stroke="#e8eef5" />
    `}).join(""),se=`
    <g>
      <line x1="${T}" y1="${f+14}" x2="${T}" y2="${F-14}" stroke="rgba(148,163,184,0.38)" stroke-width="3" stroke-linecap="round" />
      ${Array.from({length:X},(l,c)=>{const g=f+c*x+8,u=g+W/2,E=T+10,ee=m-8;return`
          <g>
            <rect x="${Y}" y="${g}" width="${J}" height="${W}" rx="14" fill="rgba(255,255,255,0.92)" stroke="rgba(191,219,254,0.92)" />
            <text x="${Y+J/2}" y="${g+18}" text-anchor="middle" font-size="12" font-weight="700" fill="#475569">${p(Le(U+c*60))}</text>
            <circle cx="${T}" cy="${u}" r="5" fill="#ffffff" stroke="#93c5fd" stroke-width="3" />
            <line x1="${E}" y1="${u}" x2="${ee}" y2="${u}" stroke="rgba(191,219,254,0.78)" stroke-width="2" stroke-linecap="round" />
          </g>
        `}).join("")}
    </g>
  `,ye=w.value.map(l=>{const c=me(l,t);if(!c)return"";const d=Ne(l),g=c.x,u=c.y,E=c.width,ee=c.height,$e=10,xe=g+3,We=c.compact;return`
      <g filter="url(#cardShadow)">
        <rect x="${g}" y="${u}" width="${E}" height="${ee}" rx="20" fill="${d.fill}" stroke="${d.stroke}" />
        <line x1="${xe}" y1="${u+$e}" x2="${xe}" y2="${u+ee-$e}" stroke="${d.accent}" stroke-width="5" stroke-linecap="round" />
        <rect x="${g+14}" y="${u+14}" width="74" height="24" rx="12" fill="${d.badge}" />
        <text x="${g+51}" y="${u+30}" text-anchor="middle" font-size="11" font-weight="600" fill="${d.badgeText}">${p(l.timeRange)}</text>
        <text x="${g+14}" y="${u+58}" font-size="17" font-weight="700" fill="#0f172a">${p(de(l.subject,14))}</text>
        ${We?"":`<text x="${g+14}" y="${u+80}" font-size="12" fill="#475569">${p(de(l.studentNamesLabel,24))}</text>`}
      </g>
    `}).join(""),P=w.value.length?"":`
      <g>
        <rect x="${m+120}" y="${f+110}" width="${S-240}" height="220" rx="36" fill="rgba(255,255,255,0.84)" stroke="#dbe7f3" />
        <text x="${e/2}" y="${f+206}" text-anchor="middle" font-size="34" font-weight="700" fill="#0f172a">${p(`${ue.value}暂无课程安排`)}</text>
        <text x="${e/2}" y="${f+246}" text-anchor="middle" font-size="16" fill="#64748b">如需新增课程，请前往排课管理。</text>
      </g>
    `,A=`
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
  `,B=`
    <svg xmlns="http://www.w3.org/2000/svg" width="${e}" height="${r}" viewBox="0 0 ${e} ${r}">
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
      <rect width="${e}" height="${r}" rx="40" fill="url(#posterBg)" />
      <rect width="${e}" height="${r}" rx="40" fill="url(#posterDots)" opacity="0.42" />
      <ellipse cx="${e-150}" cy="92" rx="230" ry="150" fill="url(#posterGlowTop)" filter="url(#softBlur)" />
      <ellipse cx="120" cy="${r-110}" rx="250" ry="180" fill="url(#posterGlowBottom)" filter="url(#softBlur)" />
      <path d="M 0 ${r-168} C 240 ${r-236}, 460 ${r-80}, 760 ${r-138} S 1290 ${r-230}, ${e} ${r-124} L ${e} ${r} L 0 ${r} Z" fill="rgba(255,255,255,0.26)" />
      <path d="M ${e-410} 54 C ${e-330} 18, ${e-232} 20, ${e-164} 82" fill="none" stroke="rgba(255,255,255,0.60)" stroke-width="3" stroke-linecap="round" />
      <path d="M 110 102 C 190 54, 302 54, 386 98" fill="none" stroke="rgba(59,130,246,0.16)" stroke-width="4" stroke-linecap="round" />
      <rect x="${a}" y="${a}" width="${Q}" height="${G}" rx="34" fill="url(#headerGlass)" stroke="rgba(255,255,255,0.88)" />
      <rect x="${a+14}" y="${a+14}" width="${Q-28}" height="${G-28}" rx="28" fill="rgba(255,255,255,0.12)" stroke="rgba(191,219,254,0.38)" />
      <rect x="${a}" y="${H}" width="${o-18}" height="${N}" rx="30" fill="url(#boardGlass)" stroke="rgba(203,213,225,0.82)" />
      <rect x="${a+10}" y="${H+14}" width="${o-38}" height="${N-28}" rx="24" fill="rgba(255,255,255,0.30)" stroke="rgba(255,255,255,0.42)" />
      <rect x="${m-18}" y="${H}" width="${S+36}" height="${N}" rx="34" fill="url(#boardGlass)" stroke="rgba(191,219,254,0.82)" />
      <circle cx="${e-96}" cy="90" r="46" fill="rgba(255,255,255,0.32)" />
      <circle cx="${e-152}" cy="136" r="18" fill="rgba(59,130,246,0.16)" />
      ${A}
      <text x="${M}" y="${a+38}" font-size="19" fill="#2563eb" font-weight="800" letter-spacing="2.8" font-family="'Avenir Next', 'Helvetica Neue', Arial, sans-serif">QINGQINGKETANG</text>
        <text x="${M}" y="${a+88}" font-size="40" fill="url(#titleInk)" font-weight="800" letter-spacing="1.1" font-family="'PingFang SC', 'Hiragino Sans GB', 'Noto Sans SC', sans-serif" filter="url(#titleShadow)">${p(ge.value)}</text>
        <text x="${M}" y="${a+120}" font-size="17" fill="#475569" font-weight="700" letter-spacing="0.9" font-family="'Avenir Next', 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif">${p(fe.value)}</text>
      ${oe}
      ${ie}
      ${se}
      ${le}
      ${ye}
      ${P}
    </svg>
  `;return Se(B)});return Ae(async()=>{await Z()}),(t,e)=>{const r=K("el-alert"),a=K("el-button"),o=K("el-image"),i=K("el-empty"),s=K("el-card");return y(),I("section",qe,[R(s,{shadow:"never",class:"schedule-board-card"},{default:z(()=>[k.value?(y(),I("div",Fe,"课表加载中…")):C.value?(y(),ne(r,{key:1,title:C.value,type:"error","show-icon":"",closable:!1,class:"schedule-board-alert"},null,8,["title"])):(y(),I("div",Qe,[ke("div",Ye,[ke("div",Je,[R(a,{class:"schedule-toolbar-button",disabled:k.value||!!b.value,onClick:e[0]||(e[0]=n=>he(-1))},{default:z(()=>[...e[2]||(e[2]=[re(" 上一周 ",-1)])]),_:1},8,["disabled"]),R(a,{class:"schedule-toolbar-button",disabled:k.value||!!b.value,onClick:Ge},{default:z(()=>[...e[3]||(e[3]=[re(" 回到本周 ",-1)])]),_:1},8,["disabled"]),R(a,{class:"schedule-toolbar-button",disabled:k.value||!!b.value,onClick:e[1]||(e[1]=n=>he(1))},{default:z(()=>[...e[4]||(e[4]=[re(" 下一周 ",-1)])]),_:1},8,["disabled"]),R(a,{class:"schedule-toolbar-button",disabled:k.value||!!b.value||!!C.value||!L.value,onClick:Me},{default:z(()=>[...e[5]||(e[5]=[re(" 打印课表 ",-1)])]),_:1},8,["disabled"])]),L.value?(y(),ne(o,{key:0,src:L.value,"preview-src-list":[L.value],class:"schedule-poster-image",fit:"contain","preview-teleported":"","hide-on-click-modal":""},null,8,["src","preview-src-list"])):Be("",!0),be.value.length?(y(),I("div",et,[(y(!0),I(Ee,null,je(be.value,n=>(y(),I("button",{key:n.groupKey,type:"button",class:"schedule-session-action","data-state":n.state,disabled:!!b.value,style:ze(n.style),"aria-label":n.ariaLabel,onClick:Re(U=>He(n),["stop"])},Ke(b.value===n.groupKey?"处理中":n.label),13,tt))),128))])):(y(),ne(i,{key:2,description:"该周暂无排课安排","image-size":72}))])]))]),_:1})])}}},st=Pe(at,[["__scopeId","data-v-7cdaa5b8"]]);export{st as default};
