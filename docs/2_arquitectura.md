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

### 2. Componentes del Backend

#### **Principales Componentes**
1. **API Gateway**
   - Punto único de entrada para solicitudes externas.
   - Maneja autenticación, autorización, y redirección de tráfico.
   - Opciones: Kong, AWS API Gateway, Nginx.

2. **Microservicio de Transacciones**
   - Gestiona ingresos, egresos y transferencias.
   - Valida lógica de negocio y CRUD sobre MongoDB.
   - Procesa entradas de voz, imágenes y texto.

3. **Microservicio de Configuración**
   - Administra los modelos principales:
     - Cuentas.
     - Categorías y subcategorías.
     - Tipos de cuentas.
     - Presupuestos.
   - Centraliza la gestión de configuraciones del sistema.

4. **Microservicio de Reportes**
   - Genera reportes financieros avanzados desde el Data Warehouse.
   - Optimiza consultas y expone APIs para análisis de datos.

5. **Servicio de Sincronización (ETL)**
   - Transferencia de datos desde MongoDB al Data Warehouse.
   - Jobs programados o sincronización en tiempo real.

#### **Componentes Complementarios**
- **Middleware Móvil**:
  - No es parte del backend central.
  - Interactúa con el backend para manejar transacciones pendientes y sincronización.

### 3. Framework y Lenguaje para el Backend

#### **Tecnología Seleccionada**: Java con Spring Boot
- **Justificación**:
  - Soporte robusto para entornos **cloud** y **on-premise**.
  - Desarrollo ágil mediante configuraciones automáticas y herramientas nativas.
  - Integración fluida con MongoDB y Data Warehouse mediante Spring Data.
  - Escalabilidad modular, ideal para microservicios.
  - Aprovechamiento de la experiencia existente del equipo en Java.


