// Plain node regression tests. Run: TZ=Europe/London node test/regression.test.cjs
const fs=require('fs'),assert=require('assert');
const html=fs.readFileSync(__dirname+'/../index.html','utf8').split(String.fromCharCode(13)).join('');
const grab=(re,w)=>{const m=html.match(re);assert(m,'not found: '+w);return m[0]};
// 1. local-date helper: 00:30 BST on 1 Jun is still 1 Jun (UTC slice would give 31 May)
const helpers=[grab(/const pad[^\n]*\n/,'pad'),grab(/const fmtKey[^\n]*\n/,'fmtKey')].join('\n');
const {fmtKey}=new Function(helpers+';return {fmtKey}')();
assert.strictEqual(fmtKey(new Date(2026,5,1,0,30)),'2026-06-01');
// 2. no UTC-derived "today" left in app code
assert(!/new Date\(\)\.toISOString\(\)\.slice\(0,10\)/.test(html),'UTC date used as today');
// 3. projectTarget span is whole days (DST-safe)
assert(/const days=Math\.round\(\(keyToDate\(last\.date\)-keyToDate\(first\.date\)\)\/86400000\)/.test(html),'projectTarget rounding');
// 4. importJSON validates backup shape
assert(/Array\.isArray\(d\.goals\)/.test(html),'import validation');
const keyToDate=grab(/const keyToDate = [\s\S]*?\n\};\n/,'keyToDate');
const fn=grab(/function projectTarget\(t\)\{[\s\S]*?\n\}\n/,'projectTarget');
const addDays=grab(/const addDays = [^\n]*\n/,'addDays');
const pt=new Function([helpers,keyToDate,addDays,fn,'return projectTarget;'].join(String.fromCharCode(10)))();
// 8 days spanning the UK autumn change: rate must be exactly 1/day, not 1/8.0416
const r=pt({history:[{date:'2026-10-20',value:0},{date:'2026-10-28',value:8}],target:20});
assert(r!==null);
assert.strictEqual(r.daysLeft,12);
assert.strictEqual(r.daysLeft,12); // (20-8)/1 per day
console.log('accountable regression: ok', JSON.stringify(r));
