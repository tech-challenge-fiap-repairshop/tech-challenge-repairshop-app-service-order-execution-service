package com.cao.repairshop.bdd.config

import io.cucumber.spring.CucumberContextConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CucumberTestcontainersConfig {

    companion object {
        init {
            System.setProperty("api.version", "1.44")
            System.setProperty("DOCKER_API_VERSION", "1.44")
        }

        private const val TABLE_NAME = "tb-service_order-test"

        val localstack: LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack:3.4.0"))
            .withServices(LocalStackContainer.Service.DYNAMODB)
            .apply {
                start()
                createDynamoTable(this)
            }

        @JvmStatic
        @DynamicPropertySource
        fun overrideProperties(registry: DynamicPropertyRegistry) {
            System.setProperty("aws.accessKeyId", localstack.accessKey)
            System.setProperty("aws.secretAccessKey", localstack.secretKey)
            System.clearProperty("aws.sessionToken")

            registry.add("aws.endpoint-url") { localstack.endpoint.toString() }
            registry.add("aws.region") { localstack.region }
            registry.add("dynamodb.table.service-order") { TABLE_NAME }
        }

        private fun createDynamoTable(container: LocalStackContainer) {
            val client = DynamoDbClient.builder()
                .endpointOverride(container.getEndpointOverride(LocalStackContainer.Service.DYNAMODB))
                .region(Region.of(container.region))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(container.accessKey, container.secretKey)
                    )
                )
                .build()

            val request = CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(
                    AttributeDefinition.builder().attributeName("PK").attributeType(ScalarAttributeType.S).build(),
                    AttributeDefinition.builder().attributeName("SK").attributeType(ScalarAttributeType.S).build(),
                    AttributeDefinition.builder().attributeName("payment_dispatch_status").attributeType(ScalarAttributeType.S).build(),
                    AttributeDefinition.builder().attributeName("update_date").attributeType(ScalarAttributeType.S).build()
                )
                .keySchema(
                    KeySchemaElement.builder().attributeName("PK").keyType(KeyType.HASH).build(),
                    KeySchemaElement.builder().attributeName("SK").keyType(KeyType.RANGE).build()
                )
                .globalSecondaryIndexes(
                    GlobalSecondaryIndex.builder()
                        .indexName("pending-dispatch-index")
                        .keySchema(
                            KeySchemaElement.builder().attributeName("payment_dispatch_status").keyType(KeyType.HASH).build(),
                            KeySchemaElement.builder().attributeName("update_date").keyType(KeyType.RANGE).build()
                        )
                        .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                        .build()
                )
                .build()

            client.createTable(request)
        }
    }
}
