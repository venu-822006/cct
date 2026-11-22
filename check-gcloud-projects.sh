#!/usr/bin/env bash
# ===================================================================
# Cloud Drive — Google Cloud Projects & Permissions Auditor
# Checks which GCP projects the current user owns and has billing on
# Usage: bash check-gcloud-projects.sh
# ===================================================================

set -e

ACCOUNT=$(gcloud config get-value account 2>/dev/null || echo "unknown")
echo "=========================================================="
echo "  Google Cloud Account Audit: $ACCOUNT"
echo "=========================================================="
echo ""

echo "--- 1. Billing Accounts ---"
gcloud billing accounts list --filter="open=true" --format="table(name,displayName,open)" || true
echo ""

echo "--- 2. Projects & Ownership Permissions ---"
printf "%-32s %-18s %-15s\n" "PROJECT_ID" "YOUR ACCESS" "BILLING"
printf "%-32s %-18s %-15s\n" "--------------------------------" "------------------" "---------------"

for p in $(gcloud projects list --format="value(projectId)" 2>/dev/null); do
  ROLES=$(gcloud projects get-iam-policy "$p" --flatten="bindings[].members" --format="value(bindings.role)" --filter="bindings.members:$ACCOUNT" 2>/dev/null | tr '\n' ',' || true)
  if echo "$ROLES" | grep -q "roles/owner"; then
    ROLE_STATUS="✓ OWNER"
  elif echo "$ROLES" | grep -q "roles/editor"; then
    ROLE_STATUS="✓ EDITOR"
  elif [ -n "$ROLES" ]; then
    ROLE_STATUS="Restricted"
  else
    ROLE_STATUS="✗ No Access"
  fi

  BILLING_INFO=$(gcloud beta billing projects describe "$p" --format="value(billingEnabled)" 2>/dev/null || echo "False")
  if [ "$BILLING_INFO" = "True" ]; then
    BILLING_STATUS="✓ ENABLED"
  else
    BILLING_STATUS="✗ Disabled"
  fi

  printf "%-32s %-18s %-15s\n" "$p" "$ROLE_STATUS" "$BILLING_STATUS"
done

echo ""
echo "=========================================================="
echo "Tip: Run 'bash deploy-gcloud.sh --project <PROJECT_ID>' with a project"
echo "     where you have '✓ OWNER' and billing is '✓ ENABLED'."
echo "     Or run 'bash deploy-gcloud.sh --create-project' to auto-create a clean project."
echo "=========================================================="
