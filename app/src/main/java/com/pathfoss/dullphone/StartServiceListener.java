package com.pathfoss.dullphone;

public interface StartServiceListener {
    void serviceStartRequested(int days, int hours, int minutes);
    void serviceStartConfirmed();
    void overlayPermissionRequested();
    void usagePermissionRequested();
}