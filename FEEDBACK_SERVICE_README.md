# Feedback Service - Documentation

## 📋 Vue d'Ensemble

Le **feedback-service** est responsable de l'analyse et du feedback IA sur les performances linguistiques des utilisateurs dans le projet WeSpeak.

### Fonctionnalités Implémentées

✅ **Transcription Automatique**
- Pipeline de traitement des enregistrements audio
- Création et gestion des transcripts
- Segments temporels avec texte et confiance

✅ **Analyse Linguistique IA**
- Analyse des performances (grammaire, vocabulaire, fluidité, prononciation)
- Génération de feedbacks détaillés avec erreurs et conseils
- Calcul automatique des scores pondérés

✅ **Gestion des XP**
- Calcul des XP basé sur le score et la durée
- Bonus pour l'amélioration
- Maximum 40 XP par session

✅ **Statistiques Utilisateur**
- Suivi de progression par langue
- Tendances (IMPROVING, STABLE, DECLINING)
- Historique des sessions

✅ **API REST Complète**
- Consultation des transcripts
- Consultation des feedbacks avec pagination
- Statistiques et historique

## 🏗️ Architecture

### Modèle de Données

```
Transcript
├── sessionId
├── participantId
├── targetLanguageCode
├── content (texte complet)
├── segments[] (timestamps + texte)
├── duration, wordCount, confidence
└── status (PENDING, PROCESSING, COMPLETED, FAILED)

Feedback
├── transcriptId
├── userId
├── sessionId
├── overallScore, grammarScore, vocabularyScore, fluencyScore
├── errors[] (type, original, correction, explanation, severity)
├── strengths[], improvements[]
├── summary
├── xpAwarded
└── status

UserFeedbackStats
├── userId, targetLanguageCode
├── totalSessions, totalMinutes
├── averageScores (overall, grammar, vocabulary, fluency)
├── commonErrors[]
└── progressTrend
```

### Stack Technique

- **Java 17** (adapté de Java 21 pour compatibilité environnement)
- **Spring Boot 4.0.0**
- **MongoDB** pour le stockage
- **Testcontainers** pour les tests d'intégration
- **Lombok** pour réduire le boilerplate

## 📡 API Endpoints

### Base URL
```
http://localhost:8084/api/v1/feedback
```

### Transcripts

#### Récupérer un transcript
```bash
GET /transcripts/{transcriptId}
Header: X-User-Id: {userId}
```

#### Lister les transcripts d'une session
```bash
GET /transcripts?sessionId={sessionId}
Header: X-User-Id: {userId}
```

### Feedbacks

#### Récupérer un feedback
```bash
GET /feedbacks/{feedbackId}
Header: X-User-Id: {userId}
```

#### Lister mes feedbacks
```bash
GET /feedbacks/me?targetLanguageCode={lang}&page={p}&size={s}
Header: X-User-Id: {userId}
```

#### Récupérer le feedback d'une session
```bash
GET /feedbacks/session/{sessionId}
Header: X-User-Id: {userId}
```

### Statistiques

#### Mes statistiques par langue
```bash
GET /stats/me?targetLanguageCode={lang}
Header: X-User-Id: {userId}
```

#### Mon historique de progression
```bash
GET /stats/me/history?targetLanguageCode={lang}&period={WEEK|MONTH|ALL}
Header: X-User-Id: {userId}
```

### Test/Debug

#### Seed des données de test
```bash
POST /seed
```

## 🧪 Tests

### Tests Unitaires
```bash
./gradlew test
```

Les tests unitaires utilisent des mocks pour tous les services externes.

### Tests d'Intégration
```bash
./gradlew integrationTest
```

Les tests d'intégration utilisent **Testcontainers** pour MongoDB. Ils nécessitent Docker pour fonctionner (disponible dans le CI).

### Tests Manuels avec cURL

```bash
# 1. Seed des données de test
curl -X POST http://localhost:8084/api/v1/seed

# 2. Récupérer mes feedbacks
curl -s "http://localhost:8084/api/v1/feedback/feedbacks/me?targetLanguageCode=en" \
  -H "X-User-Id: test-user" | python3 -m json.tool

# 3. Récupérer mes statistiques
curl -s "http://localhost:8084/api/v1/feedback/stats/me?targetLanguageCode=en" \
  -H "X-User-Id: test-user" | python3 -m json.tool

# 4. Test d'accès interdit (403)
curl -s http://localhost:8084/api/v1/feedback/feedbacks/{feedbackId} \
  -H "X-User-Id: other-user"

# 5. Test not found (404)
curl -s http://localhost:8084/api/v1/feedback/feedbacks/fake-id-999 \
  -H "X-User-Id: test-user"
```

## 🎯 Calcul des Scores et XP

### Score Global
```
overallScore = (grammarScore × 0.35) + (vocabularyScore × 0.25) 
             + (fluencyScore × 0.25) + (pronunciationScore × 0.15)
```

### Attribution des XP
```
Base: 10 XP (participation)
+ 5 XP si score ≥ 60
+ 10 XP si score ≥ 80
+ 5 XP si amélioration vs session précédente
+ 5 XP si durée ≥ 10 minutes
+ 10 XP si durée ≥ 20 minutes

Maximum: 40 XP par session
```

### Tendance de Progression
Basée sur les 5 dernières sessions :
- **IMPROVING** : Score moyen en hausse de +5 points ou plus
- **STABLE** : Variation de moins de 5 points
- **DECLINING** : Score moyen en baisse de -5 points ou plus

## 🔌 Intégrations (STUBBED)

Les intégrations suivantes sont actuellement mockées :

### Kafka
- **Consumer** : `recording.uploaded` (de conversation-service)
- **Producers** : `transcript.completed`, `feedback.generated`, `xp.awarded`

Pour déclencher le pipeline, utilisez l'endpoint `/api/v1/seed`.

### API Whisper (OpenAI)
Transcription Speech-to-Text mockée. En production :
- Modèle : `whisper-1`
- Format : segments avec timestamps
- Timeout : 120 secondes

### LLM (Claude/GPT)
Analyse linguistique mockée. En production :
- Modèle : Claude 3 Sonnet ou GPT-4
- Température : 0.3
- Timeout : 60 secondes

## 📝 Configuration

### application.properties
```properties
# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/wespeak-feedback

# Security (désactivée en dev)
app.security.enabled=false

# Port
server.port=8084
```

### application-dev.properties
```properties
# Debug logging
logging.level.org.wespeak=DEBUG

# Mock external APIs
app.whisper.mock-enabled=true
app.llm.mock-enabled=true
```

## 🚀 Démarrage

### Mode développement
```bash
./gradlew bootRun
```

### Build et run
```bash
./gradlew build
java -jar build/libs/feedback-service-1.0.0-SNAPSHOT.jar
```

## 📊 État du Projet

### ✅ Fonctionnalités Complétées
- [x] Modèles de données complets
- [x] Repositories MongoDB
- [x] Service de transcription (avec stub)
- [x] Service d'analyse IA (avec stub)
- [x] Calcul des scores et XP
- [x] Mise à jour des statistiques utilisateur
- [x] API REST complète
- [x] Gestion des erreurs (403, 404)
- [x] Tests unitaires
- [x] Tests d'intégration (Testcontainers)
- [x] Configuration dev/test
- [x] Endpoint de seed pour tests

### ⚠️ Intégrations Stubbed
- [ ] Kafka (événements)
- [ ] API Whisper (transcription)
- [ ] API LLM (analyse IA)
- [ ] S3 (récupération audio)

Ces intégrations seront implémentées dans une version future une fois les dépendances résolues (compatibilité Spring Cloud Stream avec Spring Boot 4).

## 🔍 Tests Effectués

### Tests Unitaires
✅ AnalysisServiceTest
✅ FeedbackServiceTest
✅ Mocking des dépendances

### Tests d'Intégration
✅ FeedbackServiceApplicationTests (avec Testcontainers)
⚠️ Nécessite Docker (fonctionne dans CI)

### Tests API (cURL)
✅ POST /api/v1/seed - Création données test
✅ GET /feedbacks/me - Liste paginée
✅ GET /feedbacks/{id} - Détail feedback
✅ GET /stats/me - Statistiques utilisateur
✅ GET /transcripts - Liste transcripts
✅ 403 Forbidden - Accès interdit
✅ 404 Not Found - Ressource inexistante

## 📈 Prochaines Étapes

1. **Intégration Kafka** : Une fois Spring Cloud Stream compatible avec Spring Boot 4
2. **Intégration Whisper** : Implémenter le client HTTP pour l'API OpenAI
3. **Intégration LLM** : Implémenter le client pour Claude/GPT
4. **Client S3** : Pour récupérer les fichiers audio
5. **Retry & Circuit Breaker** : Pour la résilience des appels externes
6. **Rate Limiting** : Limitation des feedbacks pour les utilisateurs free

## 🐛 Problèmes Connus

Aucun problème critique identifié. Les fonctionnalités core sont opérationnelles.

---

**Version** : 1.0.0-SNAPSHOT  
**Date** : 2026-01-05  
**Statut** : ✅ MVP Complet avec stubs pour intégrations externes
