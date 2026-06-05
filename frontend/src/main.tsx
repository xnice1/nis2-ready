import React, { useState } from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { BrowserRouter, Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { Activity, AlertTriangle, CheckCircle2, ClipboardList, FileText, LayoutDashboard, LogOut, ShieldCheck, Upload } from 'lucide-react';
import { Cell, Pie, PieChart, Radar, RadarChart, PolarGrid, PolarAngleAxis, ResponsiveContainer, Bar, BarChart, XAxis, YAxis, Tooltip } from 'recharts';
import { api, setToken, token } from './api';
import './index.css';

const queryClient = new QueryClient();
const disclaimer = 'Readiness guidance is informational only and is not legal advice, certification, or proof of official compliance.';

function Shell({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate();
  const items = [
    ['Dashboard', '/', LayoutDashboard], ['Organization', '/organization', ShieldCheck], ['Scoping', '/scoping', ClipboardList],
    ['Assessment', '/assessment', Activity], ['Controls', '/controls', CheckCircle2], ['Tasks', '/tasks', ClipboardList],
    ['Evidence', '/evidence', Upload], ['Policies', '/policies', FileText], ['Incidents', '/incidents', AlertTriangle], ['Reports', '/reports', FileText]
  ] as const;
  return <div className="min-h-screen">
    <aside className="fixed inset-y-0 left-0 hidden w-64 border-r border-stone-200 bg-white p-4 md:block">
      <div className="mb-6 text-xl font-semibold">NIS2 Ready</div>
      <nav className="space-y-1">{items.map(([label, href, Icon]) => <Link className="flex items-center gap-2 rounded px-3 py-2 text-sm hover:bg-mist" to={href} key={href}><Icon size={16}/>{label}</Link>)}</nav>
      <button className="btn-secondary mt-6 w-full" onClick={() => { setToken(null); navigate('/login'); }}><LogOut size={16}/>Sign out</button>
    </aside>
    <main className="md:ml-64">
      <header className="border-b border-stone-200 bg-white px-4 py-3 md:hidden"><div className="font-semibold">NIS2 Ready</div></header>
      <div className="mx-auto max-w-6xl p-4 md:p-8">{children}</div>
    </main>
  </div>;
}

function PublicLanding() {
  return <div className="min-h-screen bg-white">
    <section className="grid min-h-[86vh] place-items-center bg-[url('https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=1800&q=80')] bg-cover bg-center px-4 text-white">
      <div className="max-w-3xl">
        <h1 className="text-5xl font-semibold">NIS2 Ready</h1>
        <p className="mt-5 max-w-2xl text-lg">Cybersecurity readiness, gap analysis, evidence tracking, and internal preparation for European SMEs.</p>
        <p className="mt-4 max-w-2xl text-sm text-white/85">{disclaimer}</p>
        <div className="mt-8 flex gap-3"><Link className="btn" to="/register">Create workspace</Link><Link className="btn-secondary bg-white/90" to="/login">Sign in</Link></div>
      </div>
    </section>
  </div>;
}

function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [form, setForm] = useState({ email: '', password: '', firstName: '', lastName: '', organizationName: '', country: 'Poland', sector: 'Technology', employeeCountRange: '10-49', annualTurnoverRange: '<10M EUR', organizationType: 'COMPANY' });
  const submit = async (e: React.FormEvent) => {
    e.preventDefault(); setError('');
    try {
      const body = mode === 'login' ? { email: form.email, password: form.password } : form;
      const res = await api<{ token: string }>(`/auth/${mode}`, { method: 'POST', body: JSON.stringify(body) });
      setToken(res.token); navigate('/');
    } catch (err) { setError((err as Error).message); }
  };
  return <div className="grid min-h-screen place-items-center bg-mist p-4">
    <form onSubmit={submit} className="panel w-full max-w-md space-y-3">
      <h1 className="text-2xl font-semibold">{mode === 'login' ? 'Sign in' : 'Create workspace'}</h1>
      {error && <p className="rounded bg-red-50 p-2 text-sm text-red-700">{error}</p>}
      <input className="field" placeholder="Email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })}/>
      <input className="field" type="password" placeholder="Password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })}/>
      {mode === 'register' && <>
        <div className="grid grid-cols-2 gap-3"><input className="field" placeholder="First name" value={form.firstName} onChange={e => setForm({ ...form, firstName: e.target.value })}/><input className="field" placeholder="Last name" value={form.lastName} onChange={e => setForm({ ...form, lastName: e.target.value })}/></div>
        <input className="field" placeholder="Organization name" value={form.organizationName} onChange={e => setForm({ ...form, organizationName: e.target.value })}/>
        <div className="grid grid-cols-2 gap-3"><input className="field" placeholder="Country" value={form.country} onChange={e => setForm({ ...form, country: e.target.value })}/><input className="field" placeholder="Sector" value={form.sector} onChange={e => setForm({ ...form, sector: e.target.value })}/></div>
      </>}
      <button className="btn w-full">{mode === 'login' ? 'Sign in' : 'Register'}</button>
      <Link className="block text-sm text-moss" to={mode === 'login' ? '/register' : '/login'}>{mode === 'login' ? 'Need an account?' : 'Already registered?'}</Link>
    </form>
  </div>;
}

function Private({ children }: { children: React.ReactNode }) {
  return token() ? <Shell>{children}</Shell> : <Navigate to="/login" />;
}

function Dashboard() {
  const { data } = useQuery({ queryKey: ['dashboard'], queryFn: () => api<any>('/reports/dashboard') });
  const category = Object.entries(data?.categoryScores ?? {}).map(([category, score]) => ({ category, score }));
  const evidence = [{ name: 'Accepted', value: data?.acceptedEvidence ?? 0 }, { name: 'Pending', value: data?.pendingEvidence ?? 0 }];
  return <Page title="Dashboard">
    <div className="grid gap-4 md:grid-cols-4">
      <Metric label="Readiness score" value={data?.overallReadinessScore ?? 'Not assessed'} />
      <Metric label="Risk level" value={data?.riskLevel ?? 'Unknown'} />
      <Metric label="Open critical tasks" value={data?.openCriticalTasks ?? 0} />
      <Metric label="Evidence completeness" value={`${data?.evidenceCompleteness ?? 0}%`} />
    </div>
    <div className="mt-4 grid gap-4 lg:grid-cols-2">
      <div className="panel h-80"><h2 className="mb-2 font-semibold">Category readiness</h2><ResponsiveContainer><RadarChart data={category}><PolarGrid/><PolarAngleAxis dataKey="category"/><Radar dataKey="score" fill="#426b57" fillOpacity={0.45}/></RadarChart></ResponsiveContainer></div>
      <div className="panel h-80"><h2 className="mb-2 font-semibold">Evidence status</h2><ResponsiveContainer><PieChart><Pie data={evidence} dataKey="value" nameKey="name">{evidence.map((_, i) => <Cell key={i} fill={i ? '#c58a2b' : '#426b57'}/>)}</Pie><Tooltip/></PieChart></ResponsiveContainer></div>
    </div>
    <ListPanel title="Recent tasks" items={(data?.recentTasks ?? []).map((t: any) => `${t.priority}: ${t.title}`)} />
  </Page>;
}

function Organization() {
  const qc = useQueryClient();
  const { data } = useQuery({ queryKey: ['org'], queryFn: () => api<any>('/organizations/current') });
  const mutation = useMutation({ mutationFn: (body: any) => api('/organizations/current', { method: 'PUT', body: JSON.stringify(body) }), onSuccess: () => qc.invalidateQueries({ queryKey: ['org'] }) });
  const [form, setForm] = useState<any>({});
  const org = { ...data, ...form };
  return <Page title="Organization profile"><div className="panel grid gap-3 md:grid-cols-2">
    {['name','country','sector','employeeCountRange','annualTurnoverRange'].map(k => <input key={k} className="field" placeholder={k} value={org[k] ?? ''} onChange={e => setForm({ ...form, [k]: e.target.value })}/>)}
    <select className="field" value={org.organizationType ?? 'COMPANY'} onChange={e => setForm({ ...form, organizationType: e.target.value })}><option>COMPANY</option><option>CONSULTANCY</option></select>
    <button className="btn md:col-span-2" onClick={() => mutation.mutate(org)}>Save</button>
  </div></Page>;
}

function Scoping() {
  const [form, setForm] = useState<any>({ employeeCount: '10-49', turnover: '<10M EUR' });
  const [result, setResult] = useState<any>();
  const submit = async () => setResult(await api('/scoping/assess', { method: 'POST', body: JSON.stringify(form) }));
  const checks = ['providesItServices','providesManagedServices','providesDigitalInfrastructure','supportsCriticalSector','criticalSupplyChain','clientsAskForCyberDocs'];
  return <Page title="Scoping questionnaire"><div className="panel space-y-3">
    <div className="grid gap-3 md:grid-cols-2"><input className="field" placeholder="Country" onChange={e => setForm({ ...form, country: e.target.value })}/><input className="field" placeholder="Sector" onChange={e => setForm({ ...form, sector: e.target.value })}/></div>
    {checks.map(c => <label key={c} className="flex gap-2 text-sm"><input type="checkbox" onChange={e => setForm({ ...form, [c]: e.target.checked })}/>{c.replaceAll(/([A-Z])/g, ' $1')}</label>)}
    <button className="btn" onClick={submit}>Assess scope</button>
    {result && <div className="rounded bg-mist p-4"><div className="text-xl font-semibold">{result.outcome}</div><p className="mt-2 text-sm">{result.reasons.join(' ')}</p><p className="mt-2 text-xs">{result.disclaimer}</p></div>}
  </div></Page>;
}

function Assessment() {
  const qc = useQueryClient();
  const { data: questions = [] } = useQuery({ queryKey: ['questions'], queryFn: () => api<any[]>('/assessments/questions') });
  const { data: assessments = [] } = useQuery({ queryKey: ['assessments'], queryFn: () => api<any[]>('/assessments') });
  const [current, setCurrent] = useState<any>();
  const [score, setScore] = useState<any>();
  const create = async () => { const a = await api('/assessments', { method: 'POST' }); setCurrent(a); qc.invalidateQueries({ queryKey: ['assessments'] }); };
  const answer = (controlId: string, value: string) => current && api(`/assessments/${current.id}/answers`, { method: 'POST', body: JSON.stringify({ controlId, answer: value }) });
  const complete = async () => current && setScore(await api(`/assessments/${current.id}/complete`, { method: 'POST' }));
  return <Page title="Readiness assessment"><div className="mb-4 flex gap-2"><button className="btn" onClick={create}>Start assessment</button>{current && <button className="btn-secondary" onClick={complete}>Complete</button>}</div>
    {!current && <ListPanel title="Previous assessments" items={assessments.map((a: any) => `${a.status} ${a.overallScore ?? ''} ${a.createdAt}`)} />}
    {current && <div className="space-y-3">{questions.map((q: any) => <div className="panel" key={q.controlId}><div className="font-medium">{q.category}: {q.questionText}</div><p className="text-sm text-stone-600">{q.description}</p><select className="field mt-3 max-w-xs" onChange={e => answer(q.controlId, e.target.value)}><option>UNKNOWN</option><option>YES</option><option>PARTIAL</option><option>NO</option><option>NOT_APPLICABLE</option></select></div>)}</div>}
    {score && <div className="panel mt-4"><h2 className="font-semibold">Assessment result: {score.overallScore}/100, {score.riskLevel}</h2><p className="text-sm">Weakest categories: {score.weakestCategories.join(', ')}</p></div>}
  </Page>;
}

function Controls() {
  const { data = [] } = useQuery({ queryKey: ['controls'], queryFn: () => api<any[]>('/controls') });
  return <Page title="Controls checklist"><div className="grid gap-3 md:grid-cols-2">{data.map(c => <div className="panel" key={c.id}><div className="text-sm font-semibold">{c.code} · {c.category}</div><h2 className="font-medium">{c.title}</h2><p className="text-sm text-stone-600">{c.recommendedAction}</p></div>)}</div></Page>;
}

function Tasks() {
  const qc = useQueryClient();
  const { data = [] } = useQuery({ queryKey: ['tasks'], queryFn: () => api<any[]>('/tasks') });
  const update = (t: any, status: string) => api(`/tasks/${t.id}`, { method: 'PUT', body: JSON.stringify({ ...t, status }) }).then(() => qc.invalidateQueries({ queryKey: ['tasks'] }));
  const statusData = ['TODO','IN_PROGRESS','DONE','ACCEPTED_RISK','NOT_APPLICABLE'].map(s => ({ status: s, count: data.filter((t: any) => t.status === s).length }));
  return <Page title="Remediation tasks"><div className="panel h-64"><ResponsiveContainer><BarChart data={statusData}><XAxis dataKey="status"/><YAxis allowDecimals={false}/><Tooltip/><Bar dataKey="count" fill="#426b57"/></BarChart></ResponsiveContainer></div><div className="mt-4 space-y-3">{data.map((t: any) => <div className="panel flex flex-wrap items-center justify-between gap-3" key={t.id}><div><div className="font-medium">{t.priority}: {t.title}</div><p className="text-sm text-stone-600">{t.category}</p></div><select className="field max-w-48" value={t.status} onChange={e => update(t, e.target.value)}><option>TODO</option><option>IN_PROGRESS</option><option>DONE</option><option>ACCEPTED_RISK</option><option>NOT_APPLICABLE</option></select></div>)}</div></Page>;
}

function EvidencePage() {
  const qc = useQueryClient();
  const { data = [] } = useQuery({ queryKey: ['evidence'], queryFn: () => api<any[]>('/evidence') });
  const [title, setTitle] = useState('Evidence');
  const [file, setFile] = useState<File | null>(null);
  const upload = async () => { if (!file) return; const fd = new FormData(); fd.append('title', title); fd.append('file', file); await api('/evidence/upload', { method: 'POST', form: fd }); qc.invalidateQueries({ queryKey: ['evidence'] }); };
  return <Page title="Evidence library"><div className="panel flex flex-wrap gap-3"><input className="field max-w-xs" value={title} onChange={e => setTitle(e.target.value)}/><input className="field max-w-sm" type="file" onChange={e => setFile(e.target.files?.[0] ?? null)}/><button className="btn" onClick={upload}>Upload</button></div><div className="mt-4 grid gap-3 md:grid-cols-2">{data.map((e: any) => <div className="panel" key={e.id}><div className="font-medium">{e.title}</div><p className="text-sm">{e.originalFilename} · {e.status}</p></div>)}</div></Page>;
}

function Policies() {
  const qc = useQueryClient();
  const { data: templates = [] } = useQuery({ queryKey: ['templates'], queryFn: () => api<any[]>('/policy-templates') });
  const { data: policies = [] } = useQuery({ queryKey: ['policies'], queryFn: () => api<any[]>('/policies') });
  const create = (id: string) => api(`/policies/from-template/${id}`, { method: 'POST' }).then(() => qc.invalidateQueries({ queryKey: ['policies'] }));
  return <Page title="Policy templates"><div className="grid gap-4 lg:grid-cols-2"><div><h2 className="mb-2 font-semibold">Templates</h2>{templates.map((t: any) => <div className="panel mb-3" key={t.id}><div className="font-medium">{t.name}</div><p className="text-sm">{t.disclaimer}</p><button className="btn-secondary mt-2" onClick={() => create(t.id)}>Create policy</button></div>)}</div><div><h2 className="mb-2 font-semibold">Organization policies</h2>{policies.map((p: any) => <PolicyEditor key={p.id} policy={p}/>)}</div></div></Page>;
}

function PolicyEditor({ policy }: { policy: any }) {
  const qc = useQueryClient();
  const [content, setContent] = useState(policy.content);
  const save = () => api(`/policies/${policy.id}`, { method: 'PUT', body: JSON.stringify({ ...policy, content }) }).then(() => qc.invalidateQueries({ queryKey: ['policies'] }));
  return <div className="panel mb-3"><div className="font-medium">{policy.title} · {policy.status}</div><textarea className="field mt-2 min-h-40" value={content} onChange={e => setContent(e.target.value)}/><button className="btn mt-2" onClick={save}>Save</button></div>;
}

function Incidents() {
  const qc = useQueryClient();
  const { data = [] } = useQuery({ queryKey: ['incidents'], queryFn: () => api<any[]>('/incidents') });
  const create = () => api('/incidents', { method: 'POST', body: JSON.stringify({ title: 'New incident', description: 'Describe the event', severity: 'MEDIUM', generateDefaultActions: true }) }).then(() => qc.invalidateQueries({ queryKey: ['incidents'] }));
  return <Page title="Incident response"><button className="btn mb-4" onClick={create}>Create incident checklist</button><div className="space-y-3">{data.map((i: any) => <div className="panel" key={i.id}><div className="font-medium">{i.severity}: {i.title}</div><p className="text-sm">{i.status}</p></div>)}</div></Page>;
}

function Reports() {
  const { data: readiness } = useQuery({ queryKey: ['readiness-report'], queryFn: () => api<any>('/reports/readiness') });
  const { data: monthly } = useQuery({ queryKey: ['monthly-report'], queryFn: () => api<any>('/reports/monthly') });
  return <Page title="Reports"><div className="grid gap-4 lg:grid-cols-2"><JsonPanel title="Readiness report" data={readiness}/><JsonPanel title="Monthly progress" data={monthly}/></div></Page>;
}

function Page({ title, children }: { title: string; children: React.ReactNode }) {
  return <><div className="mb-6"><h1 className="text-3xl font-semibold">{title}</h1><p className="mt-1 text-sm text-stone-600">{disclaimer}</p></div>{children}</>;
}

function Metric({ label, value }: { label: string; value: React.ReactNode }) {
  return <div className="panel"><div className="text-sm text-stone-500">{label}</div><div className="mt-2 text-2xl font-semibold">{value}</div></div>;
}

function ListPanel({ title, items }: { title: string; items: string[] }) {
  return <div className="panel mt-4"><h2 className="font-semibold">{title}</h2><ul className="mt-2 space-y-1 text-sm">{items.map((x, i) => <li key={i}>{x}</li>)}</ul></div>;
}

function JsonPanel({ title, data }: { title: string; data: unknown }) {
  return <div className="panel"><h2 className="mb-2 font-semibold">{title}</h2><pre className="max-h-[34rem] overflow-auto rounded bg-mist p-3 text-xs">{JSON.stringify(data, null, 2)}</pre></div>;
}

function App() {
  return <QueryClientProvider client={queryClient}><BrowserRouter><Routes>
    <Route path="/landing" element={<PublicLanding/>}/>
    <Route path="/login" element={<AuthPage mode="login"/>}/>
    <Route path="/register" element={<AuthPage mode="register"/>}/>
    <Route path="/" element={<Private><Dashboard/></Private>}/>
    <Route path="/organization" element={<Private><Organization/></Private>}/>
    <Route path="/scoping" element={<Private><Scoping/></Private>}/>
    <Route path="/assessment" element={<Private><Assessment/></Private>}/>
    <Route path="/controls" element={<Private><Controls/></Private>}/>
    <Route path="/tasks" element={<Private><Tasks/></Private>}/>
    <Route path="/evidence" element={<Private><EvidencePage/></Private>}/>
    <Route path="/policies" element={<Private><Policies/></Private>}/>
    <Route path="/incidents" element={<Private><Incidents/></Private>}/>
    <Route path="/reports" element={<Private><Reports/></Private>}/>
    <Route path="*" element={<Navigate to={token() ? '/' : '/landing'}/>}/>
  </Routes></BrowserRouter></QueryClientProvider>;
}

ReactDOM.createRoot(document.getElementById('root')!).render(<App />);
