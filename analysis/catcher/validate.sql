.read ../prebuilt/config.sql
.read ../prebuilt/analysis_tables.sql
.read analysis.sql

.headers on
.separator "	"
SELECT * from catcher;
