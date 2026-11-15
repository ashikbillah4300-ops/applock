package com.example;

interface IShizukuPackageService {
    int hidePackage(String packageName);
    int unhidePackage(String packageName);
    boolean isPackageDisabled(String packageName);
    void destroy();
}
