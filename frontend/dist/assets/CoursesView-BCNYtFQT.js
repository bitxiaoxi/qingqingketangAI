import{a as ut}from"./api-BJ92196s.js";import{_ as yt,k as G,i as bt,o as mt,b as y,c as K,d as kt,f as wt,g as R,e as Lt,h as C,n as Mt,l as N,j as M,G as vt,H as Tt,I as It,J as St,u as _t}from"./index-BSBLDv3V.js";const Gt={class:"page-stack"},Ct={key:0,class:"page-state"},Nt={key:2,class:"schedule-poster-section"},Dt={class:"schedule-poster-frame"},Ht={__name:"CoursesView",setup(zt){const D=G(!1),v=G(""),E=G([]),H=G(bt(new Date)),f=e=>String(e??"").replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;").replace(/'/g,"&#39;"),z=(e,t)=>{const o=String(e??"");return o.length<=t?o:`${o.slice(0,Math.max(0,t-1))}…`},b=e=>{const t=new Date(e);return t.getHours()*60+t.getMinutes()},J=e=>{const t=Math.floor(e/60);return`${String(t).padStart(2,"0")}:00`},tt=e=>`data:image/svg+xml;charset=utf-8,${encodeURIComponent(e)}`,et=async()=>{D.value=!0,v.value="";try{E.value=await ut.listWeekSchedules(C(H.value))}catch(e){v.value=Mt(e,"课表加载失败")}finally{D.value=!1}},rt=M(()=>(E.value??[]).slice().sort((e,t)=>new Date(e.startTime)-new Date(t.startTime)).map(e=>{const t=String(e.status??"").toUpperCase()==="COMPLETED";return{...e,scheduleId:e.id,studentName:e.studentName??"未命名",subject:e.subject??"正式课",timeRange:_t(e.startTime,e.endTime),isCompleted:t}})),$=M(()=>{const e=new Map;return rt.value.forEach(t=>{const o=C(t.startTime),s=[o,t.startTime,t.endTime,t.subject].join("|"),a=e.get(s);if(a){a.scheduleIds.push(t.scheduleId),a.studentIds.add(t.studentId??t.scheduleId),a.studentNames.add(t.studentName),t.isCompleted?a.completedScheduleIds.push(t.scheduleId):a.pendingScheduleIds.push(t.scheduleId);return}e.set(s,{groupKey:s,dateKey:o,startTime:t.startTime,endTime:t.endTime,sortTime:new Date(t.startTime).getTime(),timeRange:t.timeRange,subject:t.subject,scheduleIds:[t.scheduleId],completedScheduleIds:t.isCompleted?[t.scheduleId]:[],pendingScheduleIds:t.isCompleted?[]:[t.scheduleId],studentIds:new Set([t.studentId??t.scheduleId]),studentNames:new Set([t.studentName])})}),Array.from(e.values()).map(t=>{const o=Array.from(t.studentNames),s=t.studentIds.size||t.scheduleIds.length,a=t.completedScheduleIds.length,p=t.pendingScheduleIds.length,m=a===t.scheduleIds.length,j=a>0&&p>0;return{groupKey:t.groupKey,dateKey:t.dateKey,startTime:t.startTime,endTime:t.endTime,sortTime:t.sortTime,timeRange:t.timeRange,subject:t.subject,scheduleIds:t.scheduleIds,participantCount:s,participantLabel:`共 ${s} 位学员`,studentNamesLabel:o.join("、"),completedCount:a,isCompleted:m,isPartial:j,sessionNote:s>1?"小组课":"单人课"}}).sort((t,o)=>t.sortTime-o.sortTime)}),T=M(()=>{const e=C(new Date);return vt(H.value).map(t=>{const o=C(t),s=t.getDay(),a=$.value.filter(p=>p.dateKey===o);return{dateKey:o,weekday:It(t),dateLabel:Tt(t),dateNumber:`${t.getDate()}`.padStart(2,"0"),monthLabel:`${t.getMonth()+1}月`,isToday:o===e,isWeekend:s===0||s===6,items:a}})}),at=M(()=>St(H.value)),A=M(()=>{if(!T.value.length)return"";const e=1600,t=36,o=122,s=12,a=Math.floor((e-t*2-o-s*6)/7),p=162,m=78,j=38,ot=8*60,it=22*60;let x=ot,I=it;if($.value.length){const r=Math.min(...$.value.map(i=>b(i.startTime))),n=Math.max(...$.value.map(i=>b(i.endTime)));x=Math.max(7*60,Math.floor(r/60)*60-60),I=Math.min(23*60,Math.ceil(n/60)*60+60),I-x<8*60&&(I=Math.min(23*60,x+8*60))}const B=Math.max(1,Math.ceil((I-x)/60)),k=84,g=t+p+m,S=B*k,l=g+S+j,h=t+o,w=t+p,st=new Map(T.value.map((r,n)=>[r.dateKey,n])),Z=7*a+6*s,V=e-t*2,U=p-24,P=w-16,W=S+m+8,_=h-24,Q=t+12,O=o-42,X=28,lt=T.value.map((r,n)=>{const i=h+n*(a+s),d=r.isToday?"#dbeafe":r.isWeekend?"#f0f9ff":"#ffffff",c=r.isToday?"#60a5fa":"#dbe7f3",L=r.items.length?`${r.items.length} 节课`:"空档";return`
      <g>
        <rect x="${i}" y="${w}" width="${a}" height="${m-12}" rx="22" fill="${d}" stroke="${c}" />
        <text x="${i+18}" y="${w+30}" font-size="16" font-weight="700" fill="#0f172a">${f(r.weekday)}</text>
        <text x="${i+18}" y="${w+54}" font-size="12" fill="#64748b">${f(`${r.monthLabel} ${r.dateNumber}`)}</text>
        <text x="${i+a-18}" y="${w+54}" text-anchor="end" font-size="12" fill="${r.isToday?"#2563eb":"#94a3b8"}">${f(r.isToday?"今天":L)}</text>
      </g>
    `}).join(""),nt=T.value.map((r,n)=>`<rect x="${h+n*(a+s)}" y="${g}" width="${a}" height="${S}" rx="26" fill="${r.isWeekend?"#f8fcff":"#ffffff"}" stroke="#e5edf5" />`).join(""),ct=Array.from({length:B+1},(r,n)=>{const i=g+n*k;return`
      <line x1="${h}" y1="${i}" x2="${h+7*a+6*s}" y2="${i}" stroke="#e8eef5" />
    `}).join(""),dt=`
    <g>
      <line x1="${_}" y1="${g+14}" x2="${_}" y2="${g+S-14}" stroke="rgba(148,163,184,0.38)" stroke-width="3" stroke-linecap="round" />
      ${Array.from({length:B},(r,n)=>{const d=g+n*k+8,c=d+X/2,L=_+10,u=h-8;return`
          <g>
            <rect x="${Q}" y="${d}" width="${O}" height="${X}" rx="14" fill="rgba(255,255,255,0.92)" stroke="rgba(191,219,254,0.92)" />
            <text x="${Q+O/2}" y="${d+18}" text-anchor="middle" font-size="12" font-weight="700" fill="#475569">${f(J(x+n*60))}</text>
            <circle cx="${_}" cy="${c}" r="5" fill="#ffffff" stroke="#93c5fd" stroke-width="3" />
            <line x1="${L}" y1="${c}" x2="${u}" y2="${c}" stroke="rgba(191,219,254,0.78)" stroke-width="2" stroke-linecap="round" />
          </g>
        `}).join("")}
    </g>
  `,ft=r=>r.isCompleted?{fill:"#f0fdf4",stroke:"#86efac",accent:"#22c55e",badge:"#dcfce7",badgeText:"#15803d"}:r.isPartial?{fill:"#eff6ff",stroke:"#93c5fd",accent:"#3b82f6",badge:"#dbeafe",badgeText:"#1d4ed8"}:{fill:"#fff7ed",stroke:"#fdba74",accent:"#f59e0b",badge:"#ffedd5",badgeText:"#c2410c"},gt=$.value.map(r=>{const n=st.get(r.dateKey);if(n===void 0)return"";const i=ft(r),d=h+n*(a+s)+10,c=g+(b(r.startTime)-x)/60*k+8,L=a-20,u=Math.max(76,(b(r.endTime)-b(r.startTime))/60*k-12),Y=10,q=d+3,F=u<102,xt=r.isCompleted?"已销课":r.isPartial?`已销 ${r.completedCount}/${r.scheduleIds.length}`:`${r.sessionNote} · ${r.participantLabel}`;return`
      <g filter="url(#cardShadow)">
        <rect x="${d}" y="${c}" width="${L}" height="${u}" rx="20" fill="${i.fill}" stroke="${i.stroke}" />
        <line x1="${q}" y1="${c+Y}" x2="${q}" y2="${c+u-Y}" stroke="${i.accent}" stroke-width="5" stroke-linecap="round" />
        <rect x="${d+14}" y="${c+14}" width="74" height="24" rx="12" fill="${i.badge}" />
        <text x="${d+51}" y="${c+30}" text-anchor="middle" font-size="11" font-weight="600" fill="${i.badgeText}">${f(r.timeRange)}</text>
        <text x="${d+14}" y="${c+58}" font-size="17" font-weight="700" fill="#0f172a">${f(z(r.subject,14))}</text>
        ${F?"":`<text x="${d+14}" y="${c+80}" font-size="12" fill="#475569">${f(z(r.studentNamesLabel,24))}</text>`}
        ${F?"":`<text x="${d+14}" y="${c+100}" font-size="11" fill="#64748b">${f(r.participantLabel)}</text>`}
        <text x="${d+14}" y="${c+u-18}" font-size="11" fill="${i.badgeText}">${f(z(xt,18))}</text>
      </g>
    `}).join(""),pt=$.value.length?"":`
      <g>
        <rect x="${h+120}" y="${g+110}" width="${Z-240}" height="220" rx="36" fill="rgba(255,255,255,0.84)" stroke="#dbe7f3" />
        <text x="${e/2}" y="${g+206}" text-anchor="middle" font-size="34" font-weight="700" fill="#0f172a">本周暂无课程安排</text>
        <text x="${e/2}" y="${g+246}" text-anchor="middle" font-size="16" fill="#64748b">如需新增课程，请前往排课管理。</text>
      </g>
    `,ht=`
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
      <g transform="translate(${e-262} ${l-180})">
        <rect x="18" y="56" width="118" height="84" rx="20" fill="rgba(255,255,255,0.88)" stroke="rgba(148,163,184,0.26)" />
        <rect x="32" y="44" width="118" height="84" rx="20" fill="rgba(219,234,254,0.88)" stroke="rgba(96,165,250,0.28)" />
        <path d="M 32 74 H 150" stroke="rgba(148,163,184,0.36)" stroke-width="4" stroke-linecap="round" />
        <path d="M 52 92 H 132" stroke="rgba(148,163,184,0.30)" stroke-width="4" stroke-linecap="round" />
        <path d="M 52 108 H 118" stroke="rgba(148,163,184,0.24)" stroke-width="4" stroke-linecap="round" />
        <circle cx="116" cy="22" r="20" fill="rgba(255,255,255,0.96)" stroke="rgba(148,163,184,0.24)" />
        <circle cx="108" cy="20" r="3.5" fill="#334155" />
        <circle cx="124" cy="20" r="3.5" fill="#334155" />
        <path d="M 108 30 Q 116 38 124 30" fill="none" stroke="#334155" stroke-width="3" stroke-linecap="round" />
        <circle cx="102" cy="28" r="4" fill="rgba(251,113,133,0.30)" />
        <circle cx="130" cy="28" r="4" fill="rgba(251,113,133,0.30)" />
      </g>
      <g opacity="0.72">
        <path d="M ${t+190} 54 L ${t+196} 42 L ${t+202} 54 L ${t+214} 60 L ${t+202} 66 L ${t+196} 78 L ${t+190} 66 L ${t+178} 60 Z" fill="rgba(253,224,71,0.82)" />
        <path d="M ${t+226} 114 L ${t+230} 106 L ${t+234} 114 L ${t+242} 118 L ${t+234} 122 L ${t+230} 130 L ${t+226} 122 L ${t+218} 118 Z" fill="rgba(125,211,252,0.88)" />
      </g>
    </g>
  `,$t=`
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
      <rect x="${t}" y="${t}" width="${V}" height="${U}" rx="34" fill="url(#headerGlass)" stroke="rgba(255,255,255,0.88)" />
      <rect x="${t+14}" y="${t+14}" width="${V-28}" height="${U-28}" rx="28" fill="rgba(255,255,255,0.12)" stroke="rgba(191,219,254,0.38)" />
      <rect x="${t}" y="${P}" width="${o-18}" height="${W}" rx="30" fill="url(#boardGlass)" stroke="rgba(203,213,225,0.82)" />
      <rect x="${t+10}" y="${P+14}" width="${o-38}" height="${W-28}" rx="24" fill="rgba(255,255,255,0.30)" stroke="rgba(255,255,255,0.42)" />
      <rect x="${h-18}" y="${P}" width="${Z+36}" height="${W}" rx="34" fill="url(#boardGlass)" stroke="rgba(191,219,254,0.82)" />
      <circle cx="${e-96}" cy="90" r="46" fill="rgba(255,255,255,0.32)" />
      <circle cx="${e-152}" cy="136" r="18" fill="rgba(59,130,246,0.16)" />
      ${ht}
      <text x="${t}" y="${t+38}" font-size="19" fill="#2563eb" font-weight="800" letter-spacing="2.8" font-family="'Avenir Next', 'Helvetica Neue', Arial, sans-serif">QINGQINGKETANG</text>
      <text x="${t}" y="${t+88}" font-size="40" fill="url(#titleInk)" font-weight="800" letter-spacing="1.1" font-family="'PingFang SC', 'Hiragino Sans GB', 'Noto Sans SC', sans-serif" filter="url(#titleShadow)">本周课程表</text>
      <text x="${t}" y="${t+120}" font-size="17" fill="#475569" font-weight="700" letter-spacing="0.9" font-family="'Avenir Next', 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif">${f(at.value)}</text>
      ${lt}
      ${nt}
      ${dt}
      ${ct}
      ${gt}
      ${pt}
    </svg>
  `;return tt($t)});return mt(async()=>{await et()}),(e,t)=>{const o=N("el-alert"),s=N("el-image"),a=N("el-empty"),p=N("el-card");return y(),K("section",Gt,[kt(p,{shadow:"never",class:"schedule-board-card"},{default:wt(()=>[D.value?(y(),K("div",Ct,"课表加载中…")):v.value?(y(),R(o,{key:1,title:v.value,type:"error","show-icon":"",closable:!1},null,8,["title"])):(y(),K("div",Nt,[Lt("div",Dt,[A.value?(y(),R(s,{key:0,src:A.value,"preview-src-list":[A.value],class:"schedule-poster-image",fit:"contain","preview-teleported":"","hide-on-click-modal":""},null,8,["src","preview-src-list"])):(y(),R(a,{key:1,description:"本周暂无排课安排","image-size":72}))])]))]),_:1})])}}},Bt=yt(Ht,[["__scopeId","data-v-49d1e259"]]);export{Bt as default};
