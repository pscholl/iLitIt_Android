iLitIt_Android
==============

Android companion Application for the iLitIt Bluetooth Lighter

Current debug build can be found here: https://github.com/pscholl/iLitIt_Android/raw/master/app/build/outputs/apk/app-debug.apk

## API: fired intents

Annotation add:

    i = new Intent("de.unifreiburg.es.iLitIt.ADD_CIG")
    i.addExtra("timestamp", <iso-string-timestamp>)
    i.addExtra("latitude", <double>)
    i.addExtra("longitude", <double>)
    i.addExtra("via", <string>) // pathway of the added annotation

Annotation remove:

    i = new Intent("de.unifreiburg.es.iLitIt.REM_CIG")
    i.addExtra("timestamp", <iso-string-timestamp>)

Clear all Annotations:

    i = new Intent("de.unifreiburg.es.iLitIt.CLR")


