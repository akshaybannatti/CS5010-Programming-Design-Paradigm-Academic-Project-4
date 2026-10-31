# build.gradle code block

jar {
    manifest {
        attributes(
            'Main-Class': 'CalendarRunner'
        )
    }
    archiveBaseName.set('f25-hw4-calendar')
    archiveVersion.set('')
}

# generate the jar file
gradle clean build

# run the jar file
java -jar build/libs/f25-hw4-calendar.jar