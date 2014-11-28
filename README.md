iLitIt_Android
==============

Android companion Application for the iLitIt Bluetooth Lighter

## API: fired intents

Annotation add:

    i = new Intent("de.unifreiburg.es.iLitIt.ADD_CIG")
    i.addExtra("timestamp", <iso-string-timestamp>)
    i.addExtra("latitude", <double>)
    i.addExtra("longitude", <double>)

Annotation remove:

    i = new Intent("de.unifreiburg.es.iLitIt.REM_CIG")
    i.addExtra("timestamp", <iso-string-timestamp>)

Clear all Annotations:

    i = new Intent("de.unifreiburg.es.iLitIt.CLR")


