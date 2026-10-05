# Smart Solar Microgrid Trading System (SolarRays) - Android Mobile App

> **SE4040 - Enterprise Application Development | Group Project**  
> **Sri Lanka Institute of Information Technology (SLIIT) — 2026**

[![License: Proprietary Academic](https://img.shields.io/badge/License-Academic%20Proprietary-red.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android%20Native%20(Java)-3DDC84.svg)](https://developer.android.com/)
[![Local DB](https://img.shields.io/badge/Local%20Database-SQLite%20(No%20Third--party%20ORM)-003B57.svg)](https://sqlite.org/)
[![Networking](https://img.shields.io/badge/Networking-Pure%20HttpURLConnection-0078D7.svg)](https://developer.android.com/)
[![Academic Integrity](https://img.shields.io/badge/Academic%20Integrity-Fingerprinted%20%26%20Protected-orange.svg)](#academic-integrity--anti-plagiarism-warning)

---

## ⚠️ Academic Integrity & Anti-Plagiarism Warning

> **IMPORTANT NOTICE TO STUDENTS AND EVALUATORS:**  
> This repository contains original academic coursework authored by the group members listed below for the module **SE4040 - Enterprise Application Development**.  
> 
> **STRICT COPYRIGHT & ANTI-COPY NOTICE:**  
> - **Plagiarism Detection:** This codebase has been submitted, fingerprinted, and registered in automated academic plagiarism detection systems (including **Turnitin**, **MOSS**, and **Codequiry**).  
> - **Zero Tolerance:** Any reproduction, cloning, reuse, or submission of this code (in whole or in part) for any coursework, assignment, or evaluation at SLIIT or any other academic institution constitutes severe academic misconduct, resulting in immediate disciplinary action and academic penalties.
> - **Official Authorship Record:** Git commit logs, PGP signatures, and timestamps on this repository establish definitive legal and academic priority.

---

## Group Members & Assigned Domains

| Member | Name | Assigned Domain | Git Author |
| :---: | :--- | :--- | :--- |
| **Member 1** | Manuga | Prosumer Mobile Profile, Hardware Specs & Deactivation | `Manuga` |
| **Member 2** | Sanjitha R. | Native Authentication, Session Storage & Login | `SanjithaRa` |
| **Member 3** | Avishka Vikum Hettige | Live Microgrid Node Discovery (RecyclerView) | `Avishka Vikum Hettige` |
| **Member 4** | D.M.R.W. Dissanayake (Rivinma Wethmini) | Energy Slot Booking Wizard, Offline SQLite Cache & QR Pass | `RivinmaWethmini` |

---

## Architectural Compliance (Academic Rubric)

- **Pure Native Android (Java):** Zero third-party ORMs (Room, Realm) or heavy networking frameworks (Retrofit, Volley) used. Built purely with native Android SDK, `HttpURLConnection`, and custom `SQLiteOpenHelper`.
- **Offline SQLite Caching:** Implements `DatabaseHelper` managing local user sessions, offline reservation caches, and nodes.
- **Strict Role-Based Security:** Direct role-bound dashboards with zero shortcut bypasses. Access is strictly governed by authenticated JWT session tokens stored in secure encrypted local storage.
- **High-Definition QR Code Pass:** Dynamic QR pass generation for approved energy dispatch verification.

---

## License

Copyright &copy; 2026 The Authors. All rights reserved.  
Distributed under the terms of the **[Academic Research & Proprietary License](LICENSE)** for evaluation by SLIIT faculty.
