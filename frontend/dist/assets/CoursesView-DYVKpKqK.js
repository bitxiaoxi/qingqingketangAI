import{a as vt}from"./api-BJ92196s.js";import{_ as Lt,k as N,i as Mt,o as Tt,b,c as K,d as rt,f as ot,g as R,e as it,s as _t,h as D,n as St,l as M,j as T,G as It,H as Ct,I as Gt,J as Nt,u as Dt}from"./index-DllpYyJg.js";const zt={class:"page-stack"},jt={key:0,class:"page-state"},At={key:2,class:"schedule-poster-section"},Ht={class:"schedule-poster-frame"},Bt={class:"schedule-poster-toolbar"},Pt={__name:"CoursesView",setup(Wt){const z=N(!1),_=N(""),Z=N([]),j=N(Mt(new Date)),p=e=>String(e??"").replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;").replace(/'/g,"&#39;"),A=(e,t)=>{const i=String(e??"");return i.length<=t?i:`${i.slice(0,Math.max(0,t-1))}…`},k=e=>{const t=new Date(e);return t.getHours()*60+t.getMinutes()},at=e=>{const t=Math.floor(e/60);return`${String(t).padStart(2,"0")}:00`},st=e=>`data:image/svg+xml;charset=utf-8,${encodeURIComponent(e)}`,lt=()=>{if(!$.value||typeof window>"u"||typeof document>"u")return;const e=document.createElement("iframe");e.setAttribute("aria-hidden","true"),e.style.position="fixed",e.style.width="0",e.style.height="0",e.style.border="0",e.style.right="0",e.style.bottom="0",document.body.appendChild(e);const t=()=>{window.setTimeout(()=>{document.body.contains(e)&&document.body.removeChild(e)},300)},i=e.contentWindow;if(!i){t();return}const a=p(`本周课程表 ${V.value}`);i.document.open(),i.document.write(`
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
          <img id="poster" src="${$.value}" alt="${a}" />
        </div>
      </body>
    </html>
  `),i.document.close();const o=i.document.getElementById("poster"),f=()=>{i.focus(),i.print()};if(i.onafterprint=t,o!=null&&o.complete){window.setTimeout(f,120);return}o==null||o.addEventListener("load",()=>{window.setTimeout(f,120)},{once:!0}),o==null||o.addEventListener("error",t,{once:!0})},nt=async()=>{z.value=!0,_.value="";try{Z.value=await vt.listWeekSchedules(D(j.value))}catch(e){_.value=St(e,"课表加载失败")}finally{z.value=!1}},ct=T(()=>(Z.value??[]).slice().sort((e,t)=>new Date(e.startTime)-new Date(t.startTime)).map(e=>{const t=String(e.status??"").toUpperCase()==="COMPLETED";return{...e,scheduleId:e.id,studentName:e.studentName??"未命名",subject:e.subject??"正式课",timeRange:Dt(e.startTime,e.endTime),isCompleted:t}})),u=T(()=>{const e=new Map;return ct.value.forEach(t=>{const i=D(t.startTime),a=[i,t.startTime,t.endTime,t.subject].join("|"),o=e.get(a);if(o){o.scheduleIds.push(t.scheduleId),o.studentIds.add(t.studentId??t.scheduleId),o.studentNames.add(t.studentName),t.isCompleted?o.completedScheduleIds.push(t.scheduleId):o.pendingScheduleIds.push(t.scheduleId);return}e.set(a,{groupKey:a,dateKey:i,startTime:t.startTime,endTime:t.endTime,sortTime:new Date(t.startTime).getTime(),timeRange:t.timeRange,subject:t.subject,scheduleIds:[t.scheduleId],completedScheduleIds:t.isCompleted?[t.scheduleId]:[],pendingScheduleIds:t.isCompleted?[]:[t.scheduleId],studentIds:new Set([t.studentId??t.scheduleId]),studentNames:new Set([t.studentName])})}),Array.from(e.values()).map(t=>{const i=Array.from(t.studentNames),a=t.studentIds.size||t.scheduleIds.length,o=t.completedScheduleIds.length,f=t.pendingScheduleIds.length,x=o===t.scheduleIds.length,H=o>0&&f>0;return{groupKey:t.groupKey,dateKey:t.dateKey,startTime:t.startTime,endTime:t.endTime,sortTime:t.sortTime,timeRange:t.timeRange,subject:t.subject,scheduleIds:t.scheduleIds,participantCount:a,participantLabel:`共 ${a} 位学员`,studentNamesLabel:i.join("、"),completedCount:o,isCompleted:x,isPartial:H,sessionNote:a>1?"小组课":"单人课"}}).sort((t,i)=>t.sortTime-i.sortTime)}),S=T(()=>{const e=D(new Date);return Ct(j.value).map(t=>{const i=D(t),a=t.getDay(),o=u.value.filter(f=>f.dateKey===i);return{dateKey:i,weekday:Nt(t),dateLabel:Gt(t),dateNumber:`${t.getDate()}`.padStart(2,"0"),monthLabel:`${t.getMonth()+1}月`,isToday:i===e,isWeekend:a===0||a===6,items:o}})}),V=T(()=>It(j.value)),$=T(()=>{if(!S.value.length)return"";const e=1600,t=36,i=122,a=12,o=Math.floor((e-t*2-i-a*6)/7),f=162,x=78,H=38,dt=8*60,ft=22*60;let m=dt,I=ft;if(u.value.length){const r=Math.min(...u.value.map(s=>k(s.startTime))),n=Math.max(...u.value.map(s=>k(s.endTime)));m=Math.max(7*60,Math.floor(r/60)*60-60),I=Math.min(23*60,Math.ceil(n/60)*60+60),I-m<8*60&&(I=Math.min(23*60,m+8*60))}const B=Math.max(1,Math.ceil((I-m)/60)),w=84,g=t+f+x,U=B*w,P=g+U,l=P+H,h=t+i,v=t+f,pt=new Map(S.value.map((r,n)=>[r.dateKey,n])),O=7*o+6*a,Q=e-t*2,X=f-24,W=t+24,C=v-16,E=P-C,G=h-24,F=t+12,Y=i-42,q=28,gt=S.value.map((r,n)=>{const s=h+n*(o+a),d=r.isToday?"#dbeafe":r.isWeekend?"#f0f9ff":"#ffffff",c=r.isToday?"#60a5fa":"#dbe7f3",L=r.items.length?`${r.items.length} 节课`:"空档";return`
      <g>
        <rect x="${s}" y="${v}" width="${o}" height="${x-12}" rx="22" fill="${d}" stroke="${c}" />
        <text x="${s+18}" y="${v+30}" font-size="16" font-weight="700" fill="#0f172a">${p(r.weekday)}</text>
        <text x="${s+18}" y="${v+54}" font-size="12" fill="#64748b">${p(`${r.monthLabel} ${r.dateNumber}`)}</text>
        <text x="${s+o-18}" y="${v+54}" text-anchor="end" font-size="12" fill="${r.isToday?"#2563eb":"#94a3b8"}">${p(r.isToday?"今天":L)}</text>
      </g>
    `}).join(""),ht=S.value.map((r,n)=>`<rect x="${h+n*(o+a)}" y="${g}" width="${o}" height="${U}" rx="26" fill="${r.isWeekend?"#f8fcff":"#ffffff"}" stroke="#e5edf5" />`).join(""),ut=Array.from({length:B+1},(r,n)=>{const s=g+n*w;return`
      <line x1="${h}" y1="${s}" x2="${h+7*o+6*a}" y2="${s}" stroke="#e8eef5" />
    `}).join(""),$t=`
    <g>
      <line x1="${G}" y1="${g+14}" x2="${G}" y2="${P-14}" stroke="rgba(148,163,184,0.38)" stroke-width="3" stroke-linecap="round" />
      ${Array.from({length:B},(r,n)=>{const d=g+n*w+8,c=d+q/2,L=G+10,y=h-8;return`
          <g>
            <rect x="${F}" y="${d}" width="${Y}" height="${q}" rx="14" fill="rgba(255,255,255,0.92)" stroke="rgba(191,219,254,0.92)" />
            <text x="${F+Y/2}" y="${d+18}" text-anchor="middle" font-size="12" font-weight="700" fill="#475569">${p(at(m+n*60))}</text>
            <circle cx="${G}" cy="${c}" r="5" fill="#ffffff" stroke="#93c5fd" stroke-width="3" />
            <line x1="${L}" y1="${c}" x2="${y}" y2="${c}" stroke="rgba(191,219,254,0.78)" stroke-width="2" stroke-linecap="round" />
          </g>
        `}).join("")}
    </g>
  `,xt=r=>r.isCompleted?{fill:"#f0fdf4",stroke:"#86efac",accent:"#22c55e",badge:"#dcfce7",badgeText:"#15803d"}:r.isPartial?{fill:"#eff6ff",stroke:"#93c5fd",accent:"#3b82f6",badge:"#dbeafe",badgeText:"#1d4ed8"}:{fill:"#fff7ed",stroke:"#fdba74",accent:"#f59e0b",badge:"#ffedd5",badgeText:"#c2410c"},mt=u.value.map(r=>{const n=pt.get(r.dateKey);if(n===void 0)return"";const s=xt(r),d=h+n*(o+a)+10,c=g+(k(r.startTime)-m)/60*w+8,L=o-20,y=Math.max(76,(k(r.endTime)-k(r.startTime))/60*w-12),J=10,tt=d+3,et=y<102,wt=r.isCompleted?"已销课":r.isPartial?`已销 ${r.completedCount}/${r.scheduleIds.length}`:`${r.sessionNote} · ${r.participantLabel}`;return`
      <g filter="url(#cardShadow)">
        <rect x="${d}" y="${c}" width="${L}" height="${y}" rx="20" fill="${s.fill}" stroke="${s.stroke}" />
        <line x1="${tt}" y1="${c+J}" x2="${tt}" y2="${c+y-J}" stroke="${s.accent}" stroke-width="5" stroke-linecap="round" />
        <rect x="${d+14}" y="${c+14}" width="74" height="24" rx="12" fill="${s.badge}" />
        <text x="${d+51}" y="${c+30}" text-anchor="middle" font-size="11" font-weight="600" fill="${s.badgeText}">${p(r.timeRange)}</text>
        <text x="${d+14}" y="${c+58}" font-size="17" font-weight="700" fill="#0f172a">${p(A(r.subject,14))}</text>
        ${et?"":`<text x="${d+14}" y="${c+80}" font-size="12" fill="#475569">${p(A(r.studentNamesLabel,24))}</text>`}
        ${et?"":`<text x="${d+14}" y="${c+100}" font-size="11" fill="#64748b">${p(r.participantLabel)}</text>`}
        <text x="${d+14}" y="${c+y-18}" font-size="11" fill="${s.badgeText}">${p(A(wt,18))}</text>
      </g>
    `}).join(""),yt=u.value.length?"":`
      <g>
        <rect x="${h+120}" y="${g+110}" width="${O-240}" height="220" rx="36" fill="rgba(255,255,255,0.84)" stroke="#dbe7f3" />
        <text x="${e/2}" y="${g+206}" text-anchor="middle" font-size="34" font-weight="700" fill="#0f172a">本周暂无课程安排</text>
        <text x="${e/2}" y="${g+246}" text-anchor="middle" font-size="16" fill="#64748b">如需新增课程，请前往排课管理。</text>
      </g>
    `,bt=`
    <g opacity="0.96">
      <g transform="translate(-54 ${l-134})" opacity="0.78">
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
      <g transform="translate(180 ${l-208}) rotate(-10)" opacity="0.84">
        <rect x="0" y="18" width="34" height="172" rx="16" fill="#fbbf24" stroke="rgba(180,83,9,0.24)" />
        <rect x="7" y="32" width="20" height="90" rx="10" fill="rgba(255,255,255,0.28)" />
        <rect x="0" y="128" width="34" height="22" rx="8" fill="#fb7185" />
        <path d="M 0 18 L 17 0 L 34 18 Z" fill="#fde68a" stroke="rgba(148,163,184,0.24)" />
        <path d="M 11 8 L 17 2 L 23 8 Z" fill="#475569" />
      </g>
      <g opacity="0.72">
        <path d="M ${t+190} 54 L ${t+196} 42 L ${t+202} 54 L ${t+214} 60 L ${t+202} 66 L ${t+196} 78 L ${t+190} 66 L ${t+178} 60 Z" fill="rgba(253,224,71,0.82)" />
        <path d="M ${t+226} 114 L ${t+230} 106 L ${t+234} 114 L ${t+242} 118 L ${t+234} 122 L ${t+230} 130 L ${t+226} 122 L ${t+218} 118 Z" fill="rgba(125,211,252,0.88)" />
      </g>
    </g>
  `,kt=`
    <svg xmlns="http://www.w3.org/2000/svg" width="${e}" height="${l}" viewBox="0 0 ${e} ${l}">
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
      <rect width="${e}" height="${l}" rx="40" fill="url(#posterBg)" />
      <rect width="${e}" height="${l}" rx="40" fill="url(#posterDots)" opacity="0.42" />
      <ellipse cx="${e-150}" cy="92" rx="230" ry="150" fill="url(#posterGlowTop)" filter="url(#softBlur)" />
      <ellipse cx="120" cy="${l-110}" rx="250" ry="180" fill="url(#posterGlowBottom)" filter="url(#softBlur)" />
      <path d="M 0 ${l-168} C 240 ${l-236}, 460 ${l-80}, 760 ${l-138} S 1290 ${l-230}, ${e} ${l-124} L ${e} ${l} L 0 ${l} Z" fill="rgba(255,255,255,0.26)" />
      <path d="M ${e-410} 54 C ${e-330} 18, ${e-232} 20, ${e-164} 82" fill="none" stroke="rgba(255,255,255,0.60)" stroke-width="3" stroke-linecap="round" />
      <path d="M 110 102 C 190 54, 302 54, 386 98" fill="none" stroke="rgba(59,130,246,0.16)" stroke-width="4" stroke-linecap="round" />
      <rect x="${t}" y="${t}" width="${Q}" height="${X}" rx="34" fill="url(#headerGlass)" stroke="rgba(255,255,255,0.88)" />
      <rect x="${t+14}" y="${t+14}" width="${Q-28}" height="${X-28}" rx="28" fill="rgba(255,255,255,0.12)" stroke="rgba(191,219,254,0.38)" />
      <rect x="${t}" y="${C}" width="${i-18}" height="${E}" rx="30" fill="url(#boardGlass)" stroke="rgba(203,213,225,0.82)" />
      <rect x="${t+10}" y="${C+14}" width="${i-38}" height="${E-28}" rx="24" fill="rgba(255,255,255,0.30)" stroke="rgba(255,255,255,0.42)" />
      <rect x="${h-18}" y="${C}" width="${O+36}" height="${E}" rx="34" fill="url(#boardGlass)" stroke="rgba(191,219,254,0.82)" />
      <circle cx="${e-96}" cy="90" r="46" fill="rgba(255,255,255,0.32)" />
      <circle cx="${e-152}" cy="136" r="18" fill="rgba(59,130,246,0.16)" />
      ${bt}
      <text x="${W}" y="${t+38}" font-size="19" fill="#2563eb" font-weight="800" letter-spacing="2.8" font-family="'Avenir Next', 'Helvetica Neue', Arial, sans-serif">QINGQINGKETANG</text>
      <text x="${W}" y="${t+88}" font-size="40" fill="url(#titleInk)" font-weight="800" letter-spacing="1.1" font-family="'PingFang SC', 'Hiragino Sans GB', 'Noto Sans SC', sans-serif" filter="url(#titleShadow)">本周课程表</text>
      <text x="${W}" y="${t+120}" font-size="17" fill="#475569" font-weight="700" letter-spacing="0.9" font-family="'Avenir Next', 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif">${p(V.value)}</text>
      ${gt}
      ${ht}
      ${$t}
      ${ut}
      ${mt}
      ${yt}
    </svg>
  `;return st(kt)});return Tt(async()=>{await nt()}),(e,t)=>{const i=M("el-alert"),a=M("el-button"),o=M("el-image"),f=M("el-empty"),x=M("el-card");return b(),K("section",zt,[rt(x,{shadow:"never",class:"schedule-board-card"},{default:ot(()=>[z.value?(b(),K("div",jt,"课表加载中…")):_.value?(b(),R(i,{key:1,title:_.value,type:"error","show-icon":"",closable:!1},null,8,["title"])):(b(),K("div",At,[it("div",Ht,[it("div",Bt,[rt(a,{class:"schedule-print-button",disabled:!$.value,onClick:lt},{default:ot(()=>[...t[0]||(t[0]=[_t(" 打印课表 ",-1)])]),_:1},8,["disabled"])]),$.value?(b(),R(o,{key:0,src:$.value,"preview-src-list":[$.value],class:"schedule-poster-image",fit:"contain","preview-teleported":"","hide-on-click-modal":""},null,8,["src","preview-src-list"])):(b(),R(f,{key:1,description:"本周暂无排课安排","image-size":72}))])]))]),_:1})])}}},Rt=Lt(Pt,[["__scopeId","data-v-48786e76"]]);export{Rt as default};
