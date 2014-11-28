iLitIt_Android
==============

Android companion Application for the iLitIt Bluetooth Lighter

## API: fired intents

Annotation via Lighter:

 i = new Intent("de.unifreiburg.es.iLitIt.LIGHTER_ADD_CIG")
 i.addExtra("timestamp", <iso-string-timestamp>)

Annotation via UI button:

 i = new Intent("de.unifreiburg.es.iLitIt.UI_ADD_CIG")
 i.addExtra("timestamp", <iso-string-timestamp>)

Removal via UI:

 i = new Intent("de.unifreiburg.es.iLitIt.UI_REM_CIG")
 i.addExtra("timestamp", <iso-string-timestamp>)


