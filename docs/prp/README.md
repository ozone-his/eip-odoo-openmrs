# PRP Installation Guide
This installation guide below assumes you already have an instance of an OpenMRS EIP based application.

1. Copy all [standard routes](../../src/main/resources/camel) to the routes folder of your EIP application
2. Copy all [obs routes](../../src/main/resources/camel/obs) to the routes folder of your EIP application
3. Copy all [prp routes](../../src/main/resources/camel/prp) to the routes folder of your EIP application
4. Copy all the properties in [this file](../../src/main/resources/config/application.properties) to your EIP 
   application properties file, read carefully the inline documentation in the file for each property and set the 
   appropriate values.
4. Copy all the properties in [this file](../../src/main/resources/config/prp/application.properties) to your
   application properties file, read carefully the inline documentation in the file for each property and set the
   appropriate values.
