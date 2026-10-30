import { useEffect, useMemo, useRef, useState } from "react";

type SourceMode = "upload" | "website";
type UploadType = "html" | "zip";
type SplashMode = "off" | "none" | "custom";
type BuildPhase = "idle" | "building" | "ready" | "error";

type BuildRecord = {
  id: string;
  name: string;
  packageName: string;
  createdAt: string;
  source: string;
};

const permissionOptions = [
  { id: "internet", label: "Internet", note: "Network access" },
  { id: "camera", label: "Camera", note: "Capture photos" },
  { id: "microphone", label: "Microphone", note: "Record audio" },
  { id: "location", label: "Location", note: "Precise location" },
  { id: "storage", label: "Storage", note: "Read local files" },
  { id: "contacts", label: "Contacts", note: "Read contacts" },
  { id: "sms", label: "SMS", note: "Read and send SMS" },
  { id: "bluetooth", label: "Bluetooth", note: "Nearby devices" },
  { id: "vibrate", label: "Vibrate", note: "Haptic feedback" },
  { id: "install", label: "Install apps", note: "Unknown sources" },
];

const phases = ["Upload", "Prepare", "Build", "Ready"];

function Icon({ name, size = 18 }: { name: string; size?: number }) {
  const common = {
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 1.7,
    strokeLinecap: "round" as const,
    strokeLinejoin: "round" as const,
    "aria-hidden": true as const,
  };

  switch (name) {
    case "apex":
      return (
        <svg {...common} viewBox="0 0 36 36" stroke="none">
          <path d="M18 2.7 34 31H2L18 2.7Z" fill="currentColor" />
          <path d="m18 17-7.7 13.3h15.4L18 17Z" fill="#101310" />
        </svg>
      );
    case "layers":
      return <svg {...common}><path d="m12 3 9 5-9 5-9-5 9-5Z" /><path d="m3 12 9 5 9-5M3 16l9 5 9-5" /></svg>;
    case "clock":
      return <svg {...common}><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></svg>;
    case "settings":
      return <svg {...common}><path d="M12 8.5a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7Z" /><path d="m19.4 15 .1.1a1.7 1.7 0 0 1-2.4 2.4l-.1-.1a1.7 1.7 0 0 0-2.9 1.2v.2a1.7 1.7 0 0 1-3.4 0v-.2a1.7 1.7 0 0 0-2.9-1.2l-.1.1a1.7 1.7 0 0 1-2.4-2.4l.1-.1a1.7 1.7 0 0 0-1.2-2.9H4a1.7 1.7 0 0 1 0-3.4h.2a1.7 1.7 0 0 0 1.2-2.9l-.1-.1a1.7 1.7 0 0 1 2.4-2.4l.1.1a1.7 1.7 0 0 0 2.9-1.2V2a1.7 1.7 0 0 1 3.4 0v.2a1.7 1.7 0 0 0 2.9 1.2l.1-.1a1.7 1.7 0 0 1 2.4 2.4l-.1.1a1.7 1.7 0 0 0 1.2 2.9h.2a1.7 1.7 0 0 1 0 3.4h-.2a1.7 1.7 0 0 0-1.2 2.9Z" transform="translate(1.5 1.5) scale(.87)" /></svg>;
    case "chevron":
      return <svg {...common}><path d="m7 10 5 5 5-5" /></svg>;
    case "upload":
      return <svg {...common}><path d="M12 16V4m0 0L7.5 8.5M12 4l4.5 4.5" /><path d="M5 14v5h14v-5" /></svg>;
    case "globe":
      return <svg {...common}><circle cx="12" cy="12" r="9" /><path d="M3 12h18M12 3a15 15 0 0 1 0 18M12 3a15 15 0 0 0 0 18" /></svg>;
    case "file":
      return <svg {...common}><path d="M13 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10Z" /><path d="M13 3v7h7M8 15h8M8 18h6" /></svg>;
    case "image":
      return <svg {...common}><rect x="3" y="3" width="18" height="18" rx="3" /><circle cx="8.5" cy="8.5" r="1.5" /><path d="m21 15-5-5L5 21" /></svg>;
    case "download":
      return <svg {...common}><path d="M12 3v12m0 0 4.5-4.5M12 15l-4.5-4.5" /><path d="M5 16v4h14v-4" /></svg>;
    case "check":
      return <svg {...common}><path d="m5 12 4 4L19 6" /></svg>;
    case "arrow":
      return <svg {...common}><path d="M5 12h14m-6-6 6 6-6 6" /></svg>;
    case "lock":
      return <svg {...common}><rect x="4" y="10" width="16" height="11" rx="2" /><path d="M8 10V7a4 4 0 1 1 8 0v3" /></svg>;
    case "shield":
      return <svg {...common}><path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z" /><path d="m9 12 2 2 4-4" /></svg>;
    case "spark":
      return <svg {...common}><path d="m12 3 1.9 5.8L20 11l-6.1 2.1L12 19l-1.9-5.9L4 11l6.1-2.2L12 3Z" /><path d="m19 15 .9 2.1L22 18l-2.1.9L19 21l-.9-2.1L16 18l2.1-.9L19 15Z" /></svg>;
    case "terminal":
      return <svg {...common}><rect x="3" y="4" width="18" height="16" rx="2" /><path d="m7 9 3 3-3 3m6 0h4" /></svg>;
    case "plus":
      return <svg {...common}><path d="M12 5v14M5 12h14" /></svg>;
    case "close":
      return <svg {...common}><path d="m6 6 12 12M18 6 6 18" /></svg>;
    case "external":
      return <svg {...common}><path d="M14 4h6v6m-10 4L20 4" /><path d="M18 13v5a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h5" /></svg>;
    default:
      return <svg {...common}><circle cx="12" cy="12" r="8" /></svg>;
  }
}

function Toggle({ checked, onChange, label }: { checked: boolean; onChange: () => void; label: string }) {
  return (
    <button
      className={`toggle ${checked ? "toggle-on" : ""}`}
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      onClick={onChange}
    >
      <span />
    </button>
  );
}

function App() {
  const [tab, setTab] = useState<"builder" | "history">("builder");
  const [sourceMode, setSourceMode] = useState<SourceMode>("upload");
  const [uploadType, setUploadType] = useState<UploadType>("html");
  const [sourceFile, setSourceFile] = useState<File | null>(null);
  const [iconFile, setIconFile] = useState<File | null>(null);
  const [googleFile, setGoogleFile] = useState<File | null>(null);
  const [splashFile, setSplashFile] = useState<File | null>(null);
  const [websiteUrl, setWebsiteUrl] = useState("");
  const [appName, setAppName] = useState("Northstar Studio");
  const [packageSuffix, setPackageSuffix] = useState("northstar");
  const [appLinksDomain, setAppLinksDomain] = useState("");
  const [permissions, setPermissions] = useState<string[]>(["internet"]);
  const [protection, setProtection] = useState({ dex: false, strings: false });
  const [cacheOffline, setCacheOffline] = useState(true);
  const [splashMode, setSplashMode] = useState<SplashMode>("none");
  const [releaseBuild, setReleaseBuild] = useState(true);
  const [openSections, setOpenSections] = useState({ advanced: false, permissions: true, protection: false, backend: false });
  const [backend, setBackend] = useState({ owner: "", repository: "", branch: "main", workflow: "build-apk.yml", token: "" });
  const [phase, setPhase] = useState<BuildPhase>("idle");
  const [currentStep, setCurrentStep] = useState(-1);
  const [logs, setLogs] = useState<string[]>([]);
  const [error, setError] = useState("");
  const [history, setHistory] = useState<BuildRecord[]>(() => {
    try {
      return JSON.parse(localStorage.getItem("apexbuilder-history") ?? "[]") as BuildRecord[];
    } catch {
      return [];
    }
  });
  const [toast, setToast] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);
  const iconRef = useRef<HTMLInputElement>(null);
  const googleRef = useRef<HTMLInputElement>(null);
  const splashRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    localStorage.setItem("apexbuilder-history", JSON.stringify(history));
  }, [history]);

  useEffect(() => {
    if (!toast) return;
    const timer = window.setTimeout(() => setToast(""), 3000);
    return () => window.clearTimeout(timer);
  }, [toast]);

  const iconUrl = useMemo(() => iconFile ? URL.createObjectURL(iconFile) : "", [iconFile]);
  useEffect(() => () => { if (iconUrl) URL.revokeObjectURL(iconUrl); }, [iconUrl]);

  const packageName = `com.${packageSuffix.trim() || "yourapp"}`;
  const isValidPackage = /^[A-Za-z][A-Za-z0-9_]*$/.test(packageSuffix.trim());
  const sourceReady = sourceMode === "website" ? /^https?:\/\//i.test(websiteUrl.trim()) : Boolean(sourceFile);
  const canBuild = sourceReady && appName.trim().length > 0 && isValidPackage && phase !== "building";

  const chooseFile = (file: File | undefined, kind: "source" | "icon" | "google" | "splash") => {
    if (!file) return;
    if (kind === "source") setSourceFile(file);
    if (kind === "icon") setIconFile(file);
    if (kind === "google") setGoogleFile(file);
    if (kind === "splash") setSplashFile(file);
  };

  const togglePermission = (permission: string) => {
    setPermissions((current) => current.includes(permission)
      ? current.filter((item) => item !== permission)
      : [...current, permission]);
  };

  const appendLog = (line: string) => {
    setLogs((current) => [...current, `${new Date().toLocaleTimeString("en-US", { hour12: false })}  ${line}`]);
  };

  const downloadConfig = () => {
    const config = {
      appName: appName.trim(),
      packageName,
      source: sourceMode === "website" ? { type: "website", url: websiteUrl.trim() } : { type: uploadType, file: sourceFile?.name ?? "" },
      appLinksDomain: appLinksDomain.trim() || null,
      googleServicesJson: googleFile?.name ?? null,
      permissions,
      protection,
      cachePagesOffline: cacheOffline,
      startup: { mode: splashMode, asset: splashFile?.name ?? null },
      signAsRelease: releaseBuild,
    };
    const blob = new Blob([JSON.stringify(config, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `${packageSuffix || "apexbuilder"}-config.json`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const startBuild = async () => {
    if (!canBuild) {
      setToast(!sourceReady ? "Choose a source to continue" : !isValidPackage ? "Use a valid package suffix" : "Add an app name");
      return;
    }
    setTab("builder");
    setPhase("building");
    setCurrentStep(0);
    setError("");
    setLogs([]);
    const shortOwner = backend.owner && backend.repository ? `${backend.owner}/${backend.repository}` : "local preview";
    setLogs([`${new Date().toLocaleTimeString("en-US", { hour12: false })}  Preparing ${appName.trim()} for ${shortOwner}`]);
    const work = [
      { step: 0, text: sourceMode === "website" ? `Validating ${websiteUrl.trim()}` : `Reading ${sourceFile?.name ?? "project source"}` },
      { step: 1, text: `Packaging ${permissions.length} permissions and app configuration` },
      { step: 2, text: "Build request preview complete. Connect the Android client to run GitHub Actions." },
    ];
    for (const item of work) {
      await new Promise((resolve) => window.setTimeout(resolve, 750));
      setCurrentStep(item.step);
      appendLog(item.text);
    }
    await new Promise((resolve) => window.setTimeout(resolve, 500));
    setCurrentStep(3);
    setPhase("ready");
    appendLog("Configuration is ready to export. APK compilation is available in the Android client.");
    const record: BuildRecord = {
      id: crypto.randomUUID(),
      name: appName.trim(),
      packageName,
      createdAt: new Date().toISOString(),
      source: sourceMode === "website" ? websiteUrl.trim() : sourceFile?.name ?? "Project upload",
    };
    setHistory((current) => [record, ...current].slice(0, 25));
  };

  const resetBuild = () => {
    setPhase("idle");
    setCurrentStep(-1);
    setLogs([]);
    setError("");
  };

  const setSection = (section: keyof typeof openSections) => {
    setOpenSections((current) => ({ ...current, [section]: !current[section] }));
  };

  const openFilePicker = (ref: React.RefObject<HTMLInputElement | null>) => ref.current?.click();

  const renderBuildScreen = () => (
    <section className="build-screen fade-in">
      <div className="build-head">
        <button type="button" className="text-button" onClick={resetBuild}>
          <span className="back-arrow"><Icon name="arrow" size={15} /></span> Back to builder
        </button>
        <span className={`build-status ${phase === "ready" ? "status-ready" : phase === "error" ? "status-error" : "status-running"}`}>
          <span className="status-dot" /> {phase === "ready" ? "Config prepared" : phase === "error" ? "Build failed" : "Preparing build"}
        </span>
      </div>
      <div className="build-intro">
        <div className="eyebrow">BUILD SESSION / 01</div>
        <h1>{phase === "ready" ? "Your build config is ready." : "Putting it together."}</h1>
        <p>{phase === "ready" ? "Export the config or make another adjustment in the builder." : "A clear view of every step, from source to package."}</p>
      </div>
      <div className="build-layout">
        <div className="progress-panel">
          <div className="panel-kicker">BUILD PIPELINE</div>
          <div className="timeline">
            {phases.map((item, index) => {
              const complete = currentStep > index || (phase === "ready" && index <= 3);
              const active = currentStep === index && phase !== "ready";
              return (
                <div className={`timeline-row ${complete ? "complete" : ""} ${active ? "active" : ""}`} key={item}>
                  <div className="timeline-marker">{complete ? <Icon name="check" size={13} /> : <span>{`0${index + 1}`}</span>}</div>
                  <div className="timeline-copy"><strong>{item}</strong><span>{index === 0 ? "Source files" : index === 1 ? "App configuration" : index === 2 ? "Cloud compilation" : "Artifact"}</span></div>
                  {active && <span className="mini-spinner" />}
                  {complete && <time>Done</time>}
                </div>
              );
            })}
          </div>
          {phase === "ready" && (
            <div className="preview-note"><Icon name="terminal" size={17} /><span>This browser preview exports the build config. Run the configured workflow from the native Android app to compile an APK.</span></div>
          )}
          {error && <div className="error-note">{error}</div>}
          <button type="button" className="primary-button build-download" disabled={phase !== "ready"} onClick={downloadConfig}>
            <Icon name="download" size={17} /> Export config.json
          </button>
        </div>
        <div className="log-panel">
          <div className="log-head"><div><Icon name="terminal" size={17} /><strong>Live output</strong></div><span>SESSION LOG</span></div>
          <div className="log-lines" aria-live="polite">
            {logs.map((line, index) => <p key={`${line}-${index}`}><span className="log-caret">&gt;</span>{line}</p>)}
            {phase === "building" && <p className="log-cursor"><span className="log-caret">&gt;</span><i /></p>}
          </div>
          <div className="log-foot"><span className="status-dot" /> {phase === "ready" ? "SESSION COMPLETE" : "STREAMING OUTPUT"}</div>
        </div>
      </div>
    </section>
  );

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-lockup">
          <div className="brand-mark"><Icon name="apex" size={28} /></div>
          <div><div className="brand-name">APEX<span>BUILDER</span></div><div className="brand-caption">WEB APP COMPILER</div></div>
        </div>
        <div className="workspace-label">WORKSPACE</div>
        <button className={`side-link ${tab === "builder" ? "selected" : ""}`} onClick={() => { setTab("builder"); resetBuild(); }} type="button">
          <Icon name="plus" size={18} /><span>New build</span>
        </button>
        <button className={`side-link ${tab === "history" ? "selected" : ""}`} onClick={() => { setTab("history"); resetBuild(); }} type="button">
          <Icon name="clock" size={18} /><span>Build history</span>{history.length > 0 && <span className="history-count">{history.length}</span>}
        </button>
        <div className="sidebar-divider" />
        <div className="workspace-label">CONNECTED REPOSITORY</div>
        <button className="repo-select" type="button" onClick={() => { setTab("builder"); setSection("backend"); }}>
          <span className="repo-mark">GH</span>
          <span className="repo-label"><strong>{backend.repository || "Connect GitHub"}</strong><small>{backend.owner || "No repository selected"}</small></span>
          <Icon name="chevron" size={15} />
        </button>
        <div className="sidebar-bottom">
          <div className="pipeline-status"><span className="status-dot" /><div><strong>Cloud pipeline</strong><small>Configure to start building</small></div></div>
          <div className="profile-row"><div className="avatar">A</div><div><strong>Workspace</strong><small>Local preview</small></div><button aria-label="Settings" type="button" onClick={() => { setTab("builder"); setSection("backend"); }}><Icon name="settings" size={17} /></button></div>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div className="breadcrumb"><span>Workspace</span><span className="slash">/</span><strong>{tab === "history" ? "Build history" : phase === "building" || phase === "ready" ? "Build session" : "New build"}</strong></div>
          <div className="topbar-right"><span className="preview-label"><span className="status-dot" /> ANDROID CLIENT PREVIEW</span><div className="topbar-avatar">A</div></div>
        </header>

        {tab === "history" ? (
          <section className="history-screen fade-in">
            <div className="page-heading"><div className="eyebrow">YOUR WORKSPACE</div><h1>Build history</h1><p>Previous configurations created in this browser.</p></div>
            {history.length === 0 ? (
              <div className="empty-state"><div className="empty-icon"><Icon name="layers" size={22} /></div><h2>No builds yet</h2><p>Your prepared builds will show up here.</p><button className="primary-button" onClick={() => setTab("builder")} type="button"><Icon name="plus" size={16} /> Create a build</button></div>
            ) : (
              <div className="history-list">{history.map((build) => <div className="history-item" key={build.id}><div className="history-icon"><Icon name="layers" size={18} /></div><div className="history-info"><strong>{build.name}</strong><span>{build.packageName} / {build.source}</span></div><time>{new Date(build.createdAt).toLocaleString()}</time><span className="history-ready"><span className="status-dot" /> Config ready</span><button className="icon-button" type="button" onClick={() => { setAppName(build.name); setPackageSuffix(build.packageName.replace(/^com\./, "")); setTab("builder"); setToast("Build details restored"); }} aria-label="Restore build"><Icon name="arrow" size={17} /></button></div>)}</div>
            )}
          </section>
        ) : phase === "building" || phase === "ready" || phase === "error" ? renderBuildScreen() : (
          <section className="builder-screen fade-in">
            <div className="page-heading heading-row"><div><div className="eyebrow">NEW PROJECT / ANDROID</div><h1>Build your app<span className="heading-period">.</span></h1><p>Turn a website or web project into a ready-to-compile Android app.</p></div><div className="step-count"><span>STEP</span><strong>01 <i>/</i> 03</strong></div></div>

            <div className="builder-grid">
              <div className="form-column">
                <section className="form-section source-section">
                  <div className="section-head"><div className="section-number">01</div><div><h2>Choose a source</h2><p>Start with a website or upload your project files.</p></div><span className="required-label">REQUIRED</span></div>
                  <div className="source-switch" role="tablist" aria-label="Source type">
                    <button className={sourceMode === "upload" ? "active" : ""} type="button" onClick={() => setSourceMode("upload")}><Icon name="upload" size={17} /> Upload project</button>
                    <button className={sourceMode === "website" ? "active" : ""} type="button" onClick={() => setSourceMode("website")}><Icon name="globe" size={17} /> Website URL</button>
                  </div>
                  {sourceMode === "upload" ? <>
                    <div className="upload-types" role="tablist" aria-label="Upload type">
                      <button className={uploadType === "html" ? "active" : ""} type="button" onClick={() => { setUploadType("html"); setSourceFile(null); }}><span className="file-type-icon html-icon">&lt;/&gt;</span><span><strong>HTML file</strong><small>One .html document</small></span>{uploadType === "html" && <Icon name="check" size={15} />}</button>
                      <button className={uploadType === "zip" ? "active" : ""} type="button" onClick={() => { setUploadType("zip"); setSourceFile(null); }}><span className="file-type-icon zip-icon"><Icon name="layers" size={17} /></span><span><strong>ZIP project</strong><small>Zipped project folder</small></span>{uploadType === "zip" && <Icon name="check" size={15} />}</button>
                    </div>
                    <input ref={inputRef} className="hidden-input" type="file" accept={uploadType === "html" ? ".html,.htm,text/html" : ".zip,application/zip,application/x-zip-compressed"} onChange={(event) => chooseFile(event.target.files?.[0], "source")} />
                    <button type="button" className={`file-drop ${sourceFile ? "file-selected" : ""}`} onClick={() => openFilePicker(inputRef)} onDragOver={(event) => event.preventDefault()} onDrop={(event) => { event.preventDefault(); chooseFile(event.dataTransfer.files[0], "source"); }}>
                      <span className="drop-icon">{sourceFile ? <Icon name="file" size={20} /> : <Icon name="upload" size={20} />}</span>
                      <span className="drop-copy"><strong>{sourceFile ? sourceFile.name : "Drop your project here"}</strong><small>{sourceFile ? `${(sourceFile.size / (1024 * 1024)).toFixed(2)} MB / Ready to package` : uploadType === "html" ? "or browse to choose an .html file" : "or browse to choose a .zip archive"}</small></span>
                      <span className="browse-button">{sourceFile ? "Replace" : "Choose file"}</span>
                    </button>
                  </> : (
                    <div className="website-input-wrap"><label htmlFor="website-url">WEBSITE ADDRESS</label><div className="url-input"><Icon name="globe" size={17} /><input id="website-url" value={websiteUrl} onChange={(event) => setWebsiteUrl(event.target.value)} placeholder="https://yourwebsite.com" type="url" /><span className="url-secure"><Icon name="lock" size={13} /> HTTPS</span></div><p className="field-hint">The site will open inside a native Android WebView.</p></div>
                  )}
                  <div className="section-foot"><Icon name="shield" size={15} /><span>The Android client uploads files directly to your configured GitHub repository.</span></div>
                </section>

                <section className="form-section details-section">
                  <div className="section-head"><div className="section-number">02</div><div><h2>App details</h2><p>Set the identity and behavior of your Android app.</p></div></div>
                  <div className="details-layout">
                    <div className="icon-upload-block">
                      <div className="field-label">APP ICON</div>
                      <input ref={iconRef} className="hidden-input" type="file" accept="image/png,image/jpeg,image/webp" onChange={(event) => chooseFile(event.target.files?.[0], "icon")} />
                      <button className="icon-upload" type="button" onClick={() => openFilePicker(iconRef)} aria-label="Upload app icon">
                        {iconUrl ? <img src={iconUrl} alt="App icon preview" /> : <span><Icon name="image" size={21} /><small>Upload</small></span>}
                        <i><Icon name="plus" size={13} /></i>
                      </button>
                      <small className="icon-hint">Square PNG</small>
                    </div>
                    <div className="field-stack">
                      <label className="field-label" htmlFor="app-name">APP NAME</label>
                      <input id="app-name" className="text-input" value={appName} maxLength={50} onChange={(event) => setAppName(event.target.value)} placeholder="My app" />
                      <label className="field-label spaced-label" htmlFor="package-suffix">PACKAGE NAME</label>
                      <div className={`package-input ${!isValidPackage ? "invalid" : ""}`}><span>com.</span><input id="package-suffix" value={packageSuffix} onChange={(event) => setPackageSuffix(event.target.value.replace(/[^A-Za-z0-9_]/g, ""))} placeholder="myapp" /><span className="package-check"><Icon name="check" size={15} /></span></div>
                      {!isValidPackage && <small className="validation-message">Use letters, numbers, or underscores. Start with a letter.</small>}
                    </div>
                  </div>

                  <div className="accordion-block">
                    <button type="button" className="accordion-trigger" onClick={() => setSection("advanced")} aria-expanded={openSections.advanced}>
                      <span className="accordion-icon"><Icon name="settings" size={17} /></span><span className="accordion-title"><strong>Advanced settings</strong><small>App links and Google services</small></span><Icon name="chevron" size={17} />
                    </button>
                    {openSections.advanced && <div className="accordion-content">
                      <label className="field-label" htmlFor="app-links">APP LINKS DOMAIN</label><input className="text-input" id="app-links" value={appLinksDomain} onChange={(event) => setAppLinksDomain(event.target.value)} placeholder="links.yourdomain.com" />
                      <input ref={googleRef} className="hidden-input" type="file" accept="application/json,.json" onChange={(event) => chooseFile(event.target.files?.[0], "google")} />
                      <div className="inline-file-row"><div><strong>Google Sign-In</strong><small>{googleFile ? googleFile.name : "Upload your google-services.json"}</small></div><button className="outline-button" type="button" onClick={() => openFilePicker(googleRef)}>{googleFile ? "Replace" : "Upload JSON"}</button></div>
                    </div>}
                  </div>

                  <div className="accordion-block permission-block">
                    <button type="button" className="accordion-trigger" onClick={() => setSection("permissions")} aria-expanded={openSections.permissions}>
                      <span className="accordion-icon"><Icon name="lock" size={17} /></span><span className="accordion-title"><strong>Permissions</strong><small>{permissions.length} selected</small></span><Icon name="chevron" size={17} />
                    </button>
                    {openSections.permissions && <div className="permission-grid">{permissionOptions.map((permission) => <div className="permission-row" key={permission.id}><div><strong>{permission.label}</strong><small>{permission.note}</small></div><Toggle label={`Enable ${permission.label}`} checked={permissions.includes(permission.id)} onChange={() => togglePermission(permission.id)} /></div>)}</div>}
                  </div>

                  <div className="accordion-block">
                    <button type="button" className="accordion-trigger" onClick={() => setSection("protection")} aria-expanded={openSections.protection}>
                      <span className="accordion-icon"><Icon name="shield" size={17} /></span><span className="accordion-title"><strong>Protection</strong><small>Obfuscation options</small></span><Icon name="chevron" size={17} />
                    </button>
                    {openSections.protection && <div className="protection-list"><div className="permission-row"><div><strong>Dex2C protection</strong><small>Native code hardening</small></div><Toggle label="Enable Dex2C protection" checked={protection.dex} onChange={() => setProtection((value) => ({ ...value, dex: !value.dex }))} /></div><div className="permission-row"><div><strong>String protection</strong><small>Encrypt app strings</small></div><Toggle label="Enable string protection" checked={protection.strings} onChange={() => setProtection((value) => ({ ...value, strings: !value.strings }))} /></div></div>}
                  </div>

                  <div className="option-row"><span className="option-leading"><span className="accordion-icon"><Icon name="layers" size={17} /></span><span><strong>Cache pages offline</strong><small>Keep pages available without a connection</small></span></span><Toggle label="Cache pages offline" checked={cacheOffline} onChange={() => setCacheOffline((value) => !value)} /></div>

                  <div className="startup-block">
                    <div className="startup-title"><span className="accordion-icon"><Icon name="spark" size={17} /></span><span><strong>Startup loading</strong><small>Choose the app's launch screen</small></span></div>
                    <div className="choice-row">{(["off", "none", "custom"] as SplashMode[]).map((mode) => <button type="button" key={mode} className={splashMode === mode ? "selected" : ""} onClick={() => setSplashMode(mode)}><span className="radio-dot" />{mode === "off" ? "Off" : mode === "none" ? "None" : "Custom"}</button>)}</div>
                    {splashMode === "custom" && <><input ref={splashRef} className="hidden-input" type="file" accept="image/*,video/mp4" onChange={(event) => chooseFile(event.target.files?.[0], "splash")} /><button type="button" className="splash-upload" onClick={() => openFilePicker(splashRef)}><Icon name="upload" size={16} />{splashFile ? splashFile.name : "Upload MP4 or image"}</button></>}
                  </div>

                  <div className="option-row sign-row"><span className="option-leading"><span className="accordion-icon"><Icon name="shield" size={17} /></span><span><strong>Sign as release build</strong><small>Prepare a release-signed APK</small></span></span><Toggle label="Sign as release build" checked={releaseBuild} onChange={() => setReleaseBuild((value) => !value)} /></div>
                </section>

                <section className="form-section backend-section">
                  <button type="button" className="backend-heading" onClick={() => setSection("backend")} aria-expanded={openSections.backend}>
                    <div className="backend-icon">GH</div><span><strong>GitHub Actions backend</strong><small>Connect a repository to compile signed APKs</small></span><span className={`backend-connection ${backend.owner && backend.repository && backend.token ? "connected" : ""}`}><span className="status-dot" />{backend.owner && backend.repository && backend.token ? "READY TO CONNECT" : "NOT CONNECTED"}</span><Icon name="chevron" size={17} />
                  </button>
                  {openSections.backend && <div className="backend-fields"><div className="backend-explainer">This browser preview does not send or store credentials. Configure your repository in the Android client, where its token is encrypted with Android Keystore.</div><div className="two-fields"><label><span className="field-label">OWNER</span><input className="text-input" value={backend.owner} onChange={(event) => setBackend((value) => ({ ...value, owner: event.target.value }))} placeholder="your-username" /></label><label><span className="field-label">REPOSITORY</span><input className="text-input" value={backend.repository} onChange={(event) => setBackend((value) => ({ ...value, repository: event.target.value }))} placeholder="web-app-builder" /></label></div><div className="two-fields"><label><span className="field-label">BRANCH</span><input className="text-input" value={backend.branch} onChange={(event) => setBackend((value) => ({ ...value, branch: event.target.value }))} placeholder="main" /></label><label><span className="field-label">WORKFLOW FILE</span><input className="text-input" value={backend.workflow} onChange={(event) => setBackend((value) => ({ ...value, workflow: event.target.value }))} placeholder="build-apk.yml" /></label></div><label><span className="field-label">PERSONAL ACCESS TOKEN</span><input className="text-input" value={backend.token} onChange={(event) => setBackend((value) => ({ ...value, token: event.target.value }))} type="password" placeholder="ghp_..." autoComplete="off" /></label></div>}
                </section>
              </div>

              <aside className="summary-column">
                <div className="summary-card">
                  <div className="summary-head"><div><div className="eyebrow">LIVE PREVIEW</div><h2>Your Android app</h2></div><span className="preview-live"><i /> LIVE</span></div>
                  <div className="phone-stage"><div className="phone-frame"><div className="phone-camera" /><div className="phone-screen"><div className="phone-status"><span>9:41</span><span>4G 100%</span></div><div className="app-preview-icon">{iconUrl ? <img src={iconUrl} alt="" /> : <Icon name="apex" size={30} />}</div><strong>{appName.trim() || "Your app name"}</strong><span className="phone-domain">{sourceMode === "website" ? websiteUrl.replace(/^https?:\/\//, "") || "yourwebsite.com" : sourceFile ? sourceFile.name : "web app preview"}</span><div className="phone-browser"><div className="phone-browser-dot" /><span>{sourceMode === "website" ? websiteUrl.replace(/^https?:\/\//, "") || "yourwebsite.com" : "app content"}</span><span>R</span></div><div className="phone-content"><div /><div /><div /><div /><div /></div><div className="phone-nav"><span /><span /><span /></div></div></div></div>
                  <div className="preview-identity"><div><span>PACKAGE ID</span><strong>{packageName}</strong></div><span className="identity-check"><Icon name="check" size={14} /></span></div>
                  <div className="summary-separator" />
                  <div className="summary-meta"><span>Source</span><strong>{sourceMode === "website" ? "Website URL" : uploadType === "html" ? "HTML document" : "ZIP project"}</strong></div>
                  <div className="summary-meta"><span>Permissions</span><strong>{permissions.length} enabled</strong></div>
                  <div className="summary-meta"><span>Build type</span><strong>{releaseBuild ? "Release / signed" : "Debug"}</strong></div>
                  <div className="preview-disclaimer"><span className="preview-disclaimer-icon"><Icon name="terminal" size={15} /></span><span>Browser preview only. Use the Android app to connect GitHub and run a cloud build.</span></div>
                </div>
                <div className="quick-tip"><span className="tip-marker">i</span><p><strong>Keep it lightweight.</strong><br />A compact project archive uploads faster and starts your build sooner.</p></div>
              </aside>
            </div>

            <div className="builder-footer"><div className="footer-hint"><span className="required-dot" /> Required fields are marked before your build is prepared.</div><button className="primary-button" type="button" onClick={startBuild} disabled={!canBuild}><span>Prepare build</span><Icon name="arrow" size={17} /></button></div>
          </section>
        )}
      </main>
      {toast && <div className="toast"><Icon name="check" size={16} />{toast}<button type="button" onClick={() => setToast("")} aria-label="Dismiss"><Icon name="close" size={14} /></button></div>}
    </div>
  );
}

export default App;