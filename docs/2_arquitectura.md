# Documento de Arquitectura - Planifi

## Decisiones de Arquitectura

### 1. Base de Datos

#### **Almacenamiento de Datos Operativos**
- **Tecnología seleccionada**: MongoDB
- **Justificación**:
  - Manejo flexible de datos semiestructurados.
  - Capacidad para almacenar documentos JSON con adjuntos (tickets, voz, imágenes).
  - Soporte nativo para backups en formato JSON.
  - Escalabilidad horizontal para grandes volúmenes de datos.
  - Modelos centrados en movimientos etiquetados, sin jerarquías de categorías.

#### **Data Warehouse para Reportes Complejos**
- **Tecnología seleccionada**: Base de datos relacional (ej. PostgreSQL, Snowflake, Azure SQL).
- **Justificación**:
  - Optimización de consultas y generación de reportes complejos.
  - Compatibilidad con herramientas de análisis como Power BI y Tableau.
  - Capacidad para diseñar tablas preprocesadas para acelerar la generación de reportes.

#### **Estrategia de Integración**
- Uso de un proceso **ETL (Extract, Transform, Load)** para sincronizar datos clave de MongoDB al Data Warehouse.
- Herramientas sugeridas para ETL: Apache NiFi, Airflow, Pentaho.

---

### 2. Arquitectura MCP-first y componentes del backend

#### **Rol del MCP (Model Context Protocol)**
- Servidor MCP como interfaz principal entre LLMs (por ejemplo ChatGPT) y el backend.
- Expone acciones estructuradas: registrar gastos/ingresos, crear etiquetas, consultar balances y presupuestos.
- Facilita flujos conversacionales y multimodales (texto, voz, imagen) sin depender de una app móvil inicial.

#### **Principales Componentes del Backend**
1. **API Gateway / MCP Adapter**
   - Punto de entrada para clientes tradicionales y puente con el servidor MCP.
   - Maneja autenticación, autorización y orquestación de rutas hacia microservicios.
   - Opciones: Kong, AWS API Gateway, Nginx.

2. **Microservicio de Transacciones**
   - Gestiona ingresos, egresos y transferencias.
   - Administra etiquetado múltiple por movimiento en lugar de categorías.
   - Procesa entradas de voz, imágenes y texto, delegando extracción a servicios de IA/LLM.

3. **Microservicio de Configuración**
   - Administra los modelos principales:
     - Cuentas.
     - Etiquetas (creación dinámica, sin jerarquía).
     - Tipos de cuentas.
     - Presupuestos por etiquetas.
   - Centraliza la gestión de configuraciones del sistema.

4. **Microservicio de Reportes**
   - Genera reportes financieros avanzados desde el Data Warehouse.
   - Optimiza consultas y expone APIs para análisis de datos.

5. **Servicio de Sincronización (ETL)**
   - Transferencia de datos desde MongoDB al Data Warehouse.
   - Jobs programados o sincronización en tiempo real.

#### **Componentes Complementarios**
- **Motor de Enriquecimiento de Imágenes/Multimodal**:
  - Soporta la extracción automática de datos desde fotos de recibos.
  - Sugiere etiquetas basadas en contenido detectado y en contexto de conversación.
- **Middleware Móvil (opcional futuro)**:
  - No es parte del alcance inicial backend-first.
  - Podrá interactuar con el backend y el MCP para manejar transacciones pendientes y sincronización.

### 3. Framework y Lenguaje para el Backend

#### **Tecnología Seleccionada**: Java con Spring Boot
- **Justificación**:
  - Soporte robusto para entornos **cloud** y **on-premise**.
  - Desarrollo ágil mediante configuraciones automáticas y herramientas nativas.
  - Integración fluida con MongoDB y Data Warehouse mediante Spring Data.
  - Escalabilidad modular, ideal para microservicios.
  - Aprovechamiento de la experiencia existente del equipo en Java.

### 4. Flujo de interacción conversacional
- El usuario interactúa en lenguaje natural a través de un canal conversacional (chat).
- El LLM interpreta la intención y llama acciones MCP para registrar movimientos, crear etiquetas o consultar información; este canal MCP es la interfaz principal en el enfoque backend-first.
- El backend valida y persiste los movimientos etiquetados; si hay imágenes, un paso de extracción multimodal obtiene monto, fecha y posibles etiquetas antes de confirmar con el usuario.
- Los registros quedan en estado pendiente cuando no hay conectividad con el backend y se sincronizan cuando vuelve la conexión; otros clientes futuros (por ejemplo, una app móvil) solo actúan como consumidores de este mismo flujo MCP.
