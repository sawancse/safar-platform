# Auto-start wrapper for the BhramanKaro admin server (vite preview on :3001).
# Launched at logon by the Scheduled Task "bhraman-admin-preview".
# Serves the prebuilt admin/dist via `vite preview` and proxies /api -> :8080.
Set-Location 'C:\Users\Win-10\safar-platform\admin'
& 'C:\Program Files\nodejs\npm.cmd' run preview *> 'C:\Users\Win-10\safar-platform\admin\admin-preview.log' 2>&1
