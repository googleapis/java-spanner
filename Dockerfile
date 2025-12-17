FROM maven:3.9-eclipse-temurin-11

WORKDIR /app

# Copy the whole repository
COPY . .

# Install dependencies (skipping tests to save time during build, we only want to run one specific test)
# We need to install the modules because it is a multi-module project
RUN mvn install -DskipTests -Dmaven.javadoc.skip=true -Dcheckstyle.skip

# Set the working directory to the module containing the test
WORKDIR /app/google-cloud-spanner

# Command to run the test
# We use exec:java to run the main class
CMD ["mvn", "exec:java", "-Dexec.mainClass=com.google.cloud.spanner.BypassPointReadTest", "-Dexec.classpathScope=test"]
