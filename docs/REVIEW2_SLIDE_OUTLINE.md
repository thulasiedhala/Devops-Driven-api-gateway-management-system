# Review-2 slide outline (17 slides)

Each slide: what to put on it. Screenshots to capture are marked **[screenshot]**.
Do not invent references or paper results. Fill the literature slides with sources you have actually read.

1. **Title and team**: project title, team lead Bobba Snehalatha Reddy, members Thulasi Edhala, U Harpita, G Bhavani, guide Ms. Anjana Devi Akurathi, Dept. of CSE-HTE.
2. **Problem and objectives**: duplicated security and traffic control in microservices, manual releases, no live view. Objectives: one secured entry point, live route management, per-route rate limiting, CI/CD, monitoring.
3. **Research gap**: tools show routing, or CI, or monitoring separately; big products are heavy. We combine them in a lightweight open-source stack. Back this with the papers you found.
4. **Updated literature review**: 4 to 6 real sources, each with author, year, what they did, limitation.
5. **System architecture**: the architecture diagram from README. One sentence per box.
6. **Tools and technologies**: React, Spring Boot, Spring Cloud Gateway, Spring Security and JWT, MySQL, Docker, GitHub Actions, Prometheus, Grafana, Postman.
7. **Module-wise progress**: the table in section 9 of `PROJECT_EXPLANATION.md`, all modules done.
8. **Database and API integration**: users and routes tables; login flow; the route sync (`/internal/routes`) from management to gateway; REST API table from README. **[screenshot]** Postman login and route list.
9. **Testing**: unit test, smoke test checks, manual tests. **[screenshot]** green GitHub Actions run with the full-stack test.
10. **Results and screenshots**: **[screenshot]** console route table, 401 response, 200 with data, 429 after the burst.
11. **Performance analysis**: **[screenshot]** Grafana dashboard after the burst test: requests per second by route, latency, 429 and 401 counts, CPU and memory.
12. **Challenges**: pick 4 rows from section 11 of `PROJECT_EXPLANATION.md`.
13. **Innovation**: section 12 of `PROJECT_EXPLANATION.md`.
14. **Current status**: all modules built, integrated, tested and running with one command.
15. **Future work**: section 13 of `PROJECT_EXPLANATION.md`.
16. **Conclusion**: one secured, observable, automatically tested gateway, with live management.
17. **References**: add the papers from slide 4, plus official documentation: Spring Cloud Gateway reference, Prometheus documentation, Grafana documentation, Docker documentation, and RFC 7519 (JSON Web Token).
