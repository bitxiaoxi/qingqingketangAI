import{_ as fe,k as M,i as nt,o as ue,a as he,b as k,c as z,d as O,f as Z,g as gt,e as At,u as st,s as ge,q as Dt,F as pe,r as me,h as lt,n as Ht,l as Y,j as b,H as $e,t as be,E as Wt,I as ye,J as xe,K as ke,L as we,C as ve}from"./index-CONoeSZE.js";import{a as pt}from"./api-DiodLyMf.js";const Le={class:"page-stack"},Se={key:0,class:"page-state schedule-board-state"},Me={key:2,class:"schedule-poster-section"},Te={class:"schedule-poster-toolbar"},Ie=["data-state","data-size","disabled","aria-label","onClick"],Ce=7*24*60*60*1e3,Et=1600,ct=10,_e={__name:"CoursesView",setup(Ge){const v=M(!1),A=M(""),D=M([]),p=M(nt(new Date)),y=M(""),X=M(null),mt=M(1);let q=0,H=null;const h=e=>String(e??"").replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;").replace(/'/g,"&#39;"),F=e=>Array.from(String(e??"")).reduce((t,a)=>t+(/[\u0000-\u00ff]/.test(a)?.55:1),0),W=(e,t)=>{const a=String(e??"");if(F(a)<=t)return a;let r="";for(const o of Array.from(a)){const i=`${r}${o}`;if(F(`${i}…`)>t)break;r=i}return`${r}…`},Bt=(e,t,a)=>{const r=(e??[]).map(n=>String(n??"").trim()).filter(Boolean);if(!r.length||t<=0||a<=0)return[];const o=[];let i=0;for(;i<r.length&&o.length<a;){let n="";for(;i<r.length;){const l=n?`、${r[i]}`:r[i];if(F(`${n}${l}`)<=t){n+=l,i+=1;continue}n||(n=W(r[i],t),i+=1);break}if(!n)break;o.push(n)}if(i<r.length&&o.length){const n=`等${r.length}位`,l=Math.max(3,t-F(n));o[o.length-1]=`${W(o[o.length-1],l)}${n}`}return o},E=e=>{const t=new Date(e);return t.getHours()*60+t.getMinutes()},Pt=e=>{const t=Math.floor(e/60);return`${String(t).padStart(2,"0")}:00`},jt=e=>`data:image/svg+xml;charset=utf-8,${encodeURIComponent(e)}`,$t=()=>{var t;const e=((t=X.value)==null?void 0:t.clientWidth)??Et;mt.value=Math.min(1,Math.max(.35,e/Et))},Rt=()=>{if(!T.value||typeof window>"u"||typeof document>"u")return;const e=document.createElement("iframe");e.setAttribute("aria-hidden","true"),e.style.position="fixed",e.style.width="0",e.style.height="0",e.style.border="0",e.style.right="0",e.style.bottom="0",document.body.appendChild(e);const t=()=>{window.setTimeout(()=>{document.body.contains(e)&&document.body.removeChild(e)},300)},a=e.contentWindow;if(!a){t();return}const r=h(`${xt.value} ${bt.value}`);a.document.open(),a.document.write(`
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
  `),a.document.close();const o=a.document.getElementById("poster"),i=()=>{a.focus(),a.print()};if(a.onafterprint=t,o!=null&&o.complete){window.setTimeout(i,120);return}o==null||o.addEventListener("load",()=>{window.setTimeout(i,120)},{once:!0}),o==null||o.addEventListener("error",t,{once:!0})},Q=async(e=p.value)=>{const t=++q;v.value=!0,A.value="",D.value=[];try{const a=await pt.listWeekSchedules(lt(e));if(t!==q)return;D.value=a??[]}catch(a){if(t!==q)return;A.value=Ht(a,"课表加载失败")}finally{t===q&&(v.value=!1)}},Kt=(e,t)=>{const a=new Set((e??[]).map(r=>String(r)));D.value=(D.value??[]).map(r=>a.has(String(r.id))?{...r,status:t}:r)},Ut=b(()=>(D.value??[]).slice().sort((e,t)=>new Date(e.startTime)-new Date(t.startTime)).map(e=>{const t=String(e.status??"").toUpperCase()==="COMPLETED";return{...e,scheduleId:e.id,studentName:e.studentName??"未命名",studentGrade:e.studentGrade??"",subject:e.subject??"正式课",timeRange:ve(e.startTime,e.endTime),isCompleted:t}})),L=b(()=>{const e=new Map;return Ut.value.forEach(t=>{const a=lt(t.startTime),r=[a,t.startTime,t.endTime,t.subject].join("|"),o=e.get(r);if(o){o.scheduleIds.push(t.scheduleId),o.studentIds.add(t.studentId??t.scheduleId),o.studentNames.add(t.studentName),t.studentGrade&&o.studentGrades.add(t.studentGrade),t.isCompleted?o.completedScheduleIds.push(t.scheduleId):o.pendingScheduleIds.push(t.scheduleId);return}e.set(r,{groupKey:r,dateKey:a,startTime:t.startTime,endTime:t.endTime,sortTime:new Date(t.startTime).getTime(),timeRange:t.timeRange,subject:t.subject,scheduleIds:[t.scheduleId],completedScheduleIds:t.isCompleted?[t.scheduleId]:[],pendingScheduleIds:t.isCompleted?[]:[t.scheduleId],studentIds:new Set([t.studentId??t.scheduleId]),studentNames:new Set([t.studentName]),studentGrades:t.studentGrade?new Set([t.studentGrade]):new Set})}),Array.from(e.values()).map(t=>{const a=Array.from(t.studentNames),r=Array.from(t.studentGrades),o=t.studentIds.size||t.scheduleIds.length,i=t.completedScheduleIds.length,n=t.pendingScheduleIds.length,l=i===t.scheduleIds.length,I=i>0&&n>0;return{groupKey:t.groupKey,dateKey:t.dateKey,startTime:t.startTime,endTime:t.endTime,sortTime:t.sortTime,timeRange:t.timeRange,subject:t.subject,scheduleIds:t.scheduleIds,completedScheduleIds:t.completedScheduleIds,pendingScheduleIds:t.pendingScheduleIds,participantCount:o,participantLabel:`共 ${o} 位学员`,studentNames:a,studentGrades:r,gradeLabel:r.join("、"),studentNamesLabel:a.join("、"),completedCount:i,pendingCount:n,isCompleted:l,isPartial:I,sessionNote:o>1?"小组课":"单人课"}}).sort((t,a)=>t.sortTime-a.sortTime)}),J=b(()=>{const e=lt(new Date);return xe(p.value).map(t=>{const a=lt(t),r=t.getDay(),o=L.value.filter(i=>i.dateKey===a);return{dateKey:a,weekday:we(t),dateLabel:ke(t),dateNumber:`${t.getDate()}`.padStart(2,"0"),monthLabel:`${t.getMonth()+1}月`,isToday:a===e,isWeekend:r===0||r===6,items:o}})}),bt=b(()=>ye(p.value)),Vt=b(()=>{const e=p.value?p.value.getTime():0,t=nt(new Date).getTime();return Math.round((e-t)/Ce)}),yt=b(()=>{const e=Vt.value;return e===0?"本周":e===-1?"上周":e===1?"下周":e>0?`${e} 周后`:`${Math.abs(e)} 周前`}),xt=b(()=>`${yt.value}课程表`),kt=e=>e.isCompleted?"已销课":"待销课",Ot=e=>e.isCompleted?`取消 ${e.timeRange} ${e.subject} 的销课`:e.isPartial?`继续为 ${e.timeRange} ${e.subject} 销课`:`为 ${e.timeRange} ${e.subject} 销课`,wt=async e=>{const t=new Date(p.value);t.setDate(t.getDate()+e*7),p.value=nt(t),await Q(p.value)},Zt=async()=>{p.value=nt(new Date),await Q(p.value)},Yt=async e=>{if(!e||y.value)return;const t=!e.isCompleted,a=t?e.pendingScheduleIds:e.completedScheduleIds;if(a.length){y.value=e.groupKey;try{for(const r of a)t?await pt.completeSchedule(r):await pt.undoCompleteSchedule(r);Kt(a,t?"COMPLETED":"PLANNED"),Wt.success(t?"已销课":"已恢复为待上课")}catch(r){Wt.error(Ht(r,t?"销课失败":"恢复待上课失败")),await Q(p.value)}finally{y.value=""}}},vt=()=>{if(!J.value.length)return null;const e=1600,t=36,a=122,r=12,o=Math.floor((e-t*2-a-r*6)/7),i=162,n=78,l=38,I=8*60,tt=22*60;let w=I,g=tt;if(L.value.length){const K=Math.min(...L.value.map(V=>E(V.startTime))),U=Math.max(...L.value.map(V=>E(V.endTime)));w=Math.max(7*60,Math.floor(K/60)*60-60),g=Math.min(23*60,Math.ceil(U/60)*60+60),g-w<8*60&&(g=Math.min(23*60,w+8*60))}const et=Math.max(1,Math.ceil((g-w)/60)),rt=84,m=t+i+n,S=et*rt,C=m+S,at=C+l,B=t+a,_=t+i,P=new Map(J.value.map((K,U)=>[K.dateKey,U])),j=7*o+6*r,G=e-t*2,ot=i-24,it=t+24,R=_-16,dt=C-R,ft=B-24,ut=t+12,ht=a-42;return{width:e,height:at,outerPadding:t,leftRail:a,columnGap:r,columnWidth:o,headerHeight:i,dayHeaderHeight:n,bottomPadding:l,visibleStart:w,visibleEnd:g,hourRows:et,hourHeight:rt,gridTop:m,gridHeight:S,gridBottom:C,gridLeft:B,dayHeaderTop:_,dayIndexMap:P,gridAreaWidth:j,titlePanelWidth:G,titlePanelHeight:ot,titleTextX:it,boardPanelY:R,boardPanelHeight:dt,timelineAxisX:ft,timelineLabelX:ut,timelineLabelWidth:ht,timelineLabelHeight:28}},Xt=e=>e.isCompleted?{fill:"#f0fdf4",stroke:"#86efac",accent:"#22c55e",badge:"#dcfce7",badgeText:"#15803d"}:e.isPartial?{fill:"#eff6ff",stroke:"#93c5fd",accent:"#3b82f6",badge:"#dbeafe",badgeText:"#1d4ed8"}:{fill:"#fff7ed",stroke:"#fdba74",accent:"#f59e0b",badge:"#ffedd5",badgeText:"#c2410c"},Lt=e=>e<=118?{size:"compact",width:52,height:20,reserve:66,fontSize:10}:{size:"default",width:60,height:24,reserve:74,fontSize:11},St=(e,t)=>{const a=t.dayIndexMap.get(e.dateKey);if(a===void 0)return null;const r=t.gridLeft+a*(t.columnWidth+t.columnGap)+10,o=t.gridTop+(E(e.startTime)-t.visibleStart)/60*t.hourHeight+8,i=t.columnWidth-20,n=Math.max(76,(E(e.endTime)-E(e.startTime))/60*t.hourHeight-12);return{x:r,y:o,width:i,height:n}},Mt=b(()=>{const e=vt();return e?L.value.map(t=>{const a=St(t,e);if(!a)return null;const r=Lt(a.height);return{...t,label:kt(t),ariaLabel:Ot(t),state:t.isCompleted?"completed":t.isPartial?"partial":"pending",size:r.size,style:{top:`${(a.y+ct)/e.height*100}%`,left:`${(a.x+a.width-ct)/e.width*100}%`}}}).filter(Boolean):[]}),qt=b(()=>({"--schedule-action-scale":`${mt.value}`})),T=b(()=>{const e=vt();if(!e)return"";const{width:t,height:a,outerPadding:r,leftRail:o,columnGap:i,columnWidth:n,dayHeaderHeight:l,visibleStart:I,hourRows:tt,hourHeight:w,gridTop:g,gridHeight:et,gridBottom:rt,gridLeft:m,dayHeaderTop:S,gridAreaWidth:C,titlePanelWidth:at,titlePanelHeight:B,titleTextX:_,boardPanelY:P,boardPanelHeight:j,timelineAxisX:G,timelineLabelX:ot,timelineLabelWidth:it,timelineLabelHeight:R}=e,dt=J.value.map((s,d)=>{const c=m+d*(n+i),f=s.isToday?"#dbeafe":s.isWeekend?"#f0f9ff":"#ffffff",u=s.isToday?"#60a5fa":"#dbe7f3",$=s.items.length?`${s.items.length} 节课`:"空档";return`
      <g>
        <rect x="${c}" y="${S}" width="${n}" height="${l-12}" rx="22" fill="${f}" stroke="${u}" />
        <text x="${c+18}" y="${S+30}" font-size="16" font-weight="700" fill="#0f172a">${h(s.weekday)}</text>
        <text x="${c+18}" y="${S+54}" font-size="12" fill="#64748b">${h(`${s.monthLabel} ${s.dateNumber}`)}</text>
        <text x="${c+n-18}" y="${S+54}" text-anchor="end" font-size="12" fill="${s.isToday?"#2563eb":"#94a3b8"}">${h(s.isToday?"今天":$)}</text>
      </g>
    `}).join(""),ft=J.value.map((s,d)=>`<rect x="${m+d*(n+i)}" y="${g}" width="${n}" height="${et}" rx="26" fill="${s.isWeekend?"#f8fcff":"#ffffff"}" stroke="#e5edf5" />`).join(""),ut=Array.from({length:tt+1},(s,d)=>{const c=g+d*w;return`
      <line x1="${m}" y1="${c}" x2="${m+7*n+6*i}" y2="${c}" stroke="#e8eef5" />
    `}).join(""),ht=`
    <g>
      <line x1="${G}" y1="${g+14}" x2="${G}" y2="${rt-14}" stroke="rgba(148,163,184,0.38)" stroke-width="3" stroke-linecap="round" />
      ${Array.from({length:tt},(s,d)=>{const f=g+d*w+8,u=f+R/2,$=G+10,N=m-8;return`
          <g>
            <rect x="${ot}" y="${f}" width="${it}" height="${R}" rx="14" fill="rgba(255,255,255,0.92)" stroke="rgba(191,219,254,0.92)" />
            <text x="${ot+it/2}" y="${f+18}" text-anchor="middle" font-size="12" font-weight="700" fill="#475569">${h(Pt(I+d*60))}</text>
            <circle cx="${G}" cy="${u}" r="5" fill="#ffffff" stroke="#93c5fd" stroke-width="3" />
            <line x1="${$}" y1="${u}" x2="${N}" y2="${u}" stroke="rgba(191,219,254,0.78)" stroke-width="2" stroke-linecap="round" />
          </g>
        `}).join("")}
    </g>
  `,Tt=L.value.map(s=>{const d=St(s,e);if(!d)return"";const c=Xt(s),f=d.x,u=d.y,$=d.width,N=d.height,x=Lt(N),Ft=kt(s),It=10,Ct=f+3,Qt=u+32,_t=f+$-ct-x.width,Gt=u+ct,Jt=u+52,te=u+69,ee=u+88,re=Math.max(1,Math.min(4,Math.floor((N-86)/14))),ae=Math.max(4,Math.floor(($-28-x.reserve)/12)),oe=Math.max(8,Math.floor(($-28)/10)),ie=Math.max(6,Math.floor(($-28)/11)),ne=Math.max(8,Math.floor(($-28)/12)),se=Bt(s.studentNames,ne,re),Nt=s.gradeLabel?W(s.gradeLabel,ie):"",zt=se.map((le,ce)=>{const de=ce===0?0:14;return`<tspan x="${f+14}" dy="${de}">${h(le)}</tspan>`}).join("");return`
      <g filter="url(#cardShadow)">
        <rect x="${f}" y="${u}" width="${$}" height="${N}" rx="20" fill="${c.fill}" stroke="${c.stroke}" />
        <line x1="${Ct}" y1="${u+It}" x2="${Ct}" y2="${u+N-It}" stroke="${c.accent}" stroke-width="5" stroke-linecap="round" />
        <text x="${f+14}" y="${Qt}" font-size="15" font-weight="700" fill="#0f172a">${h(W(s.subject,ae))}</text>
        <rect x="${_t}" y="${Gt}" width="${x.width}" height="${x.height}" rx="${x.height/2}" fill="${c.badge}" />
        <text x="${_t+x.width/2}" y="${Gt+x.height/2+x.fontSize*.35}" text-anchor="middle" font-size="${x.fontSize}" font-weight="700" fill="${c.badgeText}">${h(Ft)}</text>
        <text x="${f+14}" y="${Jt}" font-size="11" font-weight="600" fill="#64748b">${h(W(s.timeRange,oe))}</text>
        ${Nt?`<text x="${f+14}" y="${te}" font-size="12" font-weight="800" fill="#0f172a">${h(Nt)}</text>`:""}
        ${zt?`<text x="${f+14}" y="${ee}" font-size="12" fill="#475569">${zt}</text>`:""}
      </g>
    `}).join(""),K=L.value.length?"":`
      <g>
        <rect x="${m+120}" y="${g+110}" width="${C-240}" height="220" rx="36" fill="rgba(255,255,255,0.84)" stroke="#dbe7f3" />
        <text x="${t/2}" y="${g+206}" text-anchor="middle" font-size="34" font-weight="700" fill="#0f172a">${h(`${yt.value}暂无课程安排`)}</text>
        <text x="${t/2}" y="${g+246}" text-anchor="middle" font-size="16" fill="#64748b">如需新增课程，请前往排课管理。</text>
      </g>
    `,U=`
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
  `,V=`
    <svg xmlns="http://www.w3.org/2000/svg" width="${t}" height="${a}" viewBox="0 0 ${t} ${a}">
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
      <rect width="${t}" height="${a}" rx="40" fill="url(#posterBg)" />
      <rect width="${t}" height="${a}" rx="40" fill="url(#posterDots)" opacity="0.42" />
      <ellipse cx="${t-150}" cy="92" rx="230" ry="150" fill="url(#posterGlowTop)" filter="url(#softBlur)" />
      <ellipse cx="120" cy="${a-110}" rx="250" ry="180" fill="url(#posterGlowBottom)" filter="url(#softBlur)" />
      <path d="M 0 ${a-168} C 240 ${a-236}, 460 ${a-80}, 760 ${a-138} S 1290 ${a-230}, ${t} ${a-124} L ${t} ${a} L 0 ${a} Z" fill="rgba(255,255,255,0.26)" />
      <path d="M ${t-410} 54 C ${t-330} 18, ${t-232} 20, ${t-164} 82" fill="none" stroke="rgba(255,255,255,0.60)" stroke-width="3" stroke-linecap="round" />
      <path d="M 110 102 C 190 54, 302 54, 386 98" fill="none" stroke="rgba(59,130,246,0.16)" stroke-width="4" stroke-linecap="round" />
      <rect x="${r}" y="${r}" width="${at}" height="${B}" rx="34" fill="url(#headerGlass)" stroke="rgba(255,255,255,0.88)" />
      <rect x="${r+14}" y="${r+14}" width="${at-28}" height="${B-28}" rx="28" fill="rgba(255,255,255,0.12)" stroke="rgba(191,219,254,0.38)" />
      <rect x="${r}" y="${P}" width="${o-18}" height="${j}" rx="30" fill="url(#boardGlass)" stroke="rgba(203,213,225,0.82)" />
      <rect x="${r+10}" y="${P+14}" width="${o-38}" height="${j-28}" rx="24" fill="rgba(255,255,255,0.30)" stroke="rgba(255,255,255,0.42)" />
      <rect x="${m-18}" y="${P}" width="${C+36}" height="${j}" rx="34" fill="url(#boardGlass)" stroke="rgba(191,219,254,0.82)" />
      <circle cx="${t-96}" cy="90" r="46" fill="rgba(255,255,255,0.32)" />
      <circle cx="${t-152}" cy="136" r="18" fill="rgba(59,130,246,0.16)" />
      ${U}
      <text x="${_}" y="${r+38}" font-size="19" fill="#2563eb" font-weight="800" letter-spacing="2.8" font-family="'Avenir Next', 'Helvetica Neue', Arial, sans-serif">QINGQINGKETANG</text>
        <text x="${_}" y="${r+88}" font-size="40" fill="url(#titleInk)" font-weight="800" letter-spacing="1.1" font-family="'PingFang SC', 'Hiragino Sans GB', 'Noto Sans SC', sans-serif" filter="url(#titleShadow)">${h(xt.value)}</text>
        <text x="${_}" y="${r+120}" font-size="17" fill="#475569" font-weight="700" letter-spacing="0.9" font-family="'Avenir Next', 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif">${h(bt.value)}</text>
      ${dt}
      ${ft}
      ${ht}
      ${ut}
      ${Tt}
      ${K}
    </svg>
  `;return jt(V)});return ue(async()=>{$t(),typeof ResizeObserver<"u"&&X.value&&(H=new ResizeObserver(()=>{$t()}),H.observe(X.value)),await Q()}),he(()=>{H==null||H.disconnect()}),(e,t)=>{const a=Y("el-alert"),r=Y("el-button"),o=Y("el-image"),i=Y("el-empty"),n=Y("el-card");return k(),z("section",Le,[O(n,{shadow:"never",class:"schedule-board-card"},{default:Z(()=>[v.value?(k(),z("div",Se,"课表加载中…")):A.value?(k(),gt(a,{key:1,title:A.value,type:"error","show-icon":"",closable:!1,class:"schedule-board-alert"},null,8,["title"])):(k(),z("div",Me,[At("div",{ref_key:"schedulePosterFrameRef",ref:X,class:"schedule-poster-frame"},[At("div",Te,[O(r,{class:"schedule-toolbar-button",disabled:v.value||!!y.value,onClick:t[0]||(t[0]=l=>wt(-1))},{default:Z(()=>[...t[2]||(t[2]=[st(" 上一周 ",-1)])]),_:1},8,["disabled"]),O(r,{class:"schedule-toolbar-button",disabled:v.value||!!y.value,onClick:Zt},{default:Z(()=>[...t[3]||(t[3]=[st(" 回到本周 ",-1)])]),_:1},8,["disabled"]),O(r,{class:"schedule-toolbar-button",disabled:v.value||!!y.value,onClick:t[1]||(t[1]=l=>wt(1))},{default:Z(()=>[...t[4]||(t[4]=[st(" 下一周 ",-1)])]),_:1},8,["disabled"]),O(r,{class:"schedule-toolbar-button",disabled:v.value||!!y.value||!!A.value||!T.value,onClick:Rt},{default:Z(()=>[...t[5]||(t[5]=[st(" 打印课表 ",-1)])]),_:1},8,["disabled"])]),T.value?(k(),gt(o,{key:0,src:T.value,"preview-src-list":[T.value],class:"schedule-poster-image",fit:"contain","preview-teleported":"","hide-on-click-modal":""},null,8,["src","preview-src-list"])):ge("",!0),Mt.value.length?(k(),z("div",{key:1,class:"schedule-poster-action-layer",style:Dt(qt.value)},[(k(!0),z(pe,null,me(Mt.value,l=>(k(),z("button",{key:l.groupKey,type:"button",class:"schedule-session-action","data-state":l.state,"data-size":l.size,disabled:!!y.value,style:Dt(l.style),"aria-label":l.ariaLabel,onClick:$e(I=>Yt(l),["stop"])},be(y.value===l.groupKey?"处理中":l.label),13,Ie))),128))],4)):(k(),gt(i,{key:2,description:"该周暂无排课安排","image-size":72}))],512)]))]),_:1})])}}},Ae=fe(_e,[["__scopeId","data-v-f3d4d3f4"]]);export{Ae as default};
