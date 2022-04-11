Java:

```java
private String getAppProcessName() {
        String processName = "";
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            final int ownPid = android.os.Process.myPid();
            final List<ActivityManager.RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
            if (processes != null) {
                for (ActivityManager.RunningAppProcessInfo info : processes) {
                    if (info.pid == ownPid) {
                        processName = info.processName;
                        break;
                    }
                }
            }
        } else {
            processName = Application.getProcessName();
        }
        return processName;
    }
```
