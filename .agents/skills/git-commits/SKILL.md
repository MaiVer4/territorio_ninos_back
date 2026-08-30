---
name: git-commits
description: >-
  Generates semantic Git Conventional Commit messages based on code changes, git diffs, or descriptions. Use when creating commits, formatting commit messages, preparing pull requests, or standardizing git history.
---

# 📦 Git Conventional Commits Skill

Este skill guía la generación, estructuración y validación de mensajes de commit semánticos bajo la especificación **Conventional Commits 1.0.0**, garantizando un historial de Git limpio, trazable y listo para generación automática de changelogs y versionado semántico (SemVer).

---

## 🎯 Estructura Estándar de un Commit

```text
<tipo>(<ámbito opcional>): <descripción corta en imperativo>

[cuerpo opcional: explicación detallada del porqué y contexto del cambio]

[pie opcional: referencias a tickets, issues o BREAKING CHANGE]
```

### Reglas Clave:
1. **Línea de Asunto (Subject):**
   - Máximo **50 a 72 caracteres**.
   - En tiempo presente e imperativo (ej. `add`, `fix`, `update`, `refactor` o en español `agrega`, `corrige`, `actualiza`).
   - Todo en minúsculas (salvo nombres propios/acrónimos).
   - **Sin punto final**.
2. **Cuerpo (Body - Opcional):**
   - Separado por una línea en blanco del asunto.
   - Explica el **qué** y el **por qué**, no el cómo (el cómo está en el diff).
   - Usa viñetas `-` para desglosar múltiples cambios relacionados.
3. **Pie (Footer - Opcional):**
   - `BREAKING CHANGE: <descripción del cambio incompatible>`.
   - `Closes #123`, `Fixes #456`, `Refs #789`.

---

## 🏷️ Clasificación de Tipos (Commit Types)

| Tipo | Cuándo Usarlo | Ejemplo |
| :--- | :--- | :--- |
| **`feat`** | Nueva característica o funcionalidad para el usuario final. | `feat(tickets): add kpi metrics summary cards to dashboard` |
| **`fix`** | Corrección de un bug o error en producción/desarrollo. | `fix(auth): handle expired refresh token redirect loop` |
| **`refactor`** | Reestructuración de código sin alterar funcionalidad ni corregir bugs. | `refactor(tickets): extract table filters into standalone component` |
| **`style`** | Cambios visuales, microinteracciones, estilos CSS/SCSS o formato. | `style(ui): add smooth transitions and hover elevation to cards` |
| **`perf`** | Optimizaciones de rendimiento, velocidad o consumo de memoria. | `perf(table): implement virtual scrolling for large ticket lists` |
| **`docs`** | Modificaciones en documentación, README, JSDoc o comentarios. | `docs(readme): add environment setup and running instructions` |
| **`test`** | Añadir, corregir o actualizar pruebas unitarias/e2e. | `test(auth): add unit tests for role guard authorization` |
| **`build`** | Cambios en herramientas de construcción o dependencias (`angular.json`, `package.json`, `tsconfig`). | `build(deps): bump @angular/material to v21.2.14` |
| **`ci`** | Modificaciones en pipelines de integración/despliegue continuo (GitHub Actions, Docker). | `ci(workflow): add automated test and lint check on pr` |
| **`chore`** | Tareas misceláneas de mantenimiento, configuración de linter, `.gitignore`, etc. | `chore(lint): update prettier rules for scss files` |
| **`revert`** | Reversión de un commit previo. | `revert: feat(tickets): add export to csv button` |

---

## 🧭 Guía para el Ámbito (Scope)

El ámbito identifica la sección del proyecto afectada. Debe ser conciso y en minúsculas:
- **Por módulo/feature:** `(auth)`, `(tickets)`, `(users)`, `(notifications)`, `(dashboard)`.
- **Por capa técnica:** `(api)`, `(guards)`, `(interceptors)`, `(models)`, `(ui)`, `(tokens)`.
- **Por infraestructura:** `(deps)`, `(config)`, `(ci)`, `(docker)`.

---

## 📋 Instrucciones de Ejecución Paso a Paso

Cuando el usuario solicite generar o revisar mensajes de commit:

1. **Analizar los cambios actuales:**
   - Inspeccionar el `git status` y `git diff` (o los fragmentos de código/descripción que proporcione el usuario).
   - Identificar si los cambios corresponden a una sola responsabilidad o si es aconsejable separarlos en commits atómicos.

2. **Clasificar el tipo y determinar el ámbito:**
   - Seleccionar el tipo más preciso de la tabla de tipos.
   - Definir el ámbito (`scope`) que mejor localice la modificación.

3. **Redactar el mensaje estructurado:**
   - Crear el encabezado principal: `<tipo>(<ámbito>): <descripción>`.
   - Si el cambio abarca múltiples componentes o lógica no trivial, agregar un cuerpo con viñetas explicativas.
   - Si introduce una incompatibilidad con versiones anteriores, marcar claramente `BREAKING CHANGE:`.

4. **Justificar la elección:**
   - Explicar brevemente por qué se seleccionó ese tipo y ámbito.

---

## 📤 Formato de Respuesta Recomendado

```markdown
### 📝 Propuesta de Commit

```git
<tipo>(<ámbito>): <descripción concisa>

- <Punto clave 1>
- <Punto clave 2>
```

#### 💡 Justificación:
- **Tipo (`<tipo>`):** [Explicación de por qué aplica este tipo sobre otros].
- **Ámbito (`<ámbito>`):** [Razón por la que este módulo/capa engloba el cambio].
- **Comando listo para ejecutar:**
```bash
git add .
git commit -m "<tipo>(<ámbito>): <descripción concisa>"
```
```
