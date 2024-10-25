# Use the official Azul Zulu JDK 21 image
FROM azul/zulu-openjdk:21

# Set the working directory
WORKDIR /app

# Copy the project files into the Docker image
COPY . /app

# Run Maven wrapper to build and test the project
RUN chmod +x ./mvnw
RUN ./mvnw clean verify

# Specify the command to run the application (if applicable)
# CMD ["java", "-jar", "target/your-app.jar"]

# Use this CMD if the project creates a JAR file as the main output.
