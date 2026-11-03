package com.opspilot.common.config;

import com.opspilot.approval.entity.ApprovalTask;
import com.opspilot.approval.repository.ApprovalTaskRepository;
import com.opspilot.identity.entity.Role;
import com.opspilot.identity.entity.User;
import com.opspilot.identity.repository.UserRepository;
import com.opspilot.invoice.entity.ApprovalStatus;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.InvoiceLine;
import com.opspilot.invoice.entity.ProcessingStatus;
import com.opspilot.invoice.entity.ValidationStatus;
import com.opspilot.invoice.repository.InvoiceRepository;
import com.opspilot.organization.entity.Organization;
import com.opspilot.organization.entity.OrganizationMembership;
import com.opspilot.organization.repository.OrganizationMembershipRepository;
import com.opspilot.organization.repository.OrganizationRepository;
import com.opspilot.purchaseorder.entity.PurchaseOrder;
import com.opspilot.purchaseorder.entity.PurchaseOrderLine;
import com.opspilot.purchaseorder.repository.PurchaseOrderRepository;
import com.opspilot.supplier.entity.Supplier;
import com.opspilot.supplier.repository.SupplierRepository;
import com.opspilot.workflow.service.WorkflowEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final ApprovalTaskRepository approvalTaskRepository;
    private final WorkflowEngineService workflowEngineService;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository,
            SupplierRepository supplierRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            InvoiceRepository invoiceRepository,
            ApprovalTaskRepository approvalTaskRepository,
            WorkflowEngineService workflowEngineService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.invoiceRepository = invoiceRepository;
        this.approvalTaskRepository = approvalTaskRepository;
        this.workflowEngineService = workflowEngineService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (organizationRepository.count() > 0) {
            log.info("Database already seeded. Skipping initial data seed.");
            return;
        }

        log.info("Starting initial seed of realistic Accounts Payable operational data...");

        // 1. Organization
        Organization org = new Organization("Global Logistics Enterprise", "global-logistics");
        org.setDefaultCurrency("USD");
        org = organizationRepository.save(org);

        // 2. Users with Distinct Roles
        String encodedPass = passwordEncoder.encode("Password123!");

        User admin = userRepository.save(new User("admin@opspilot.io", encodedPass, "Sarah", "Connor"));
        membershipRepository.save(new OrganizationMembership(org.getId(), admin.getId(), Role.OWNER));

        User approver = userRepository.save(new User("approver@opspilot.io", encodedPass, "Michael", "Scott"));
        membershipRepository.save(new OrganizationMembership(org.getId(), approver.getId(), Role.APPROVER));

        User operator = userRepository.save(new User("operator@opspilot.io", encodedPass, "Pam", "Beesly"));
        membershipRepository.save(new OrganizationMembership(org.getId(), operator.getId(), Role.OPERATOR));

        User viewer = userRepository.save(new User("viewer@opspilot.io", encodedPass, "Jim", "Halpert"));
        membershipRepository.save(new OrganizationMembership(org.getId(), viewer.getId(), Role.VIEWER));

        // 3. Workflow Seed
        workflowEngineService.seedDefaultWorkflowForOrganization(org.getId(), admin.getId());

        // 4. Suppliers
        Supplier s1 = supplierRepository.save(new Supplier(
                org.getId(),
                "SUP-ACME-01",
                "Acme Industrial Supplies Ltd",
                "US-99238472",
                "billing@acme.com",
                "100 Industrial Parkway, Austin, TX",
                "USD"
        ));

        Supplier s2 = supplierRepository.save(new Supplier(
                org.getId(),
                "SUP-CLOUD-02",
                "CloudScale Hosting Inc",
                "US-11823901",
                "accounts@cloudscale.io",
                "500 Server Drive, Seattle, WA",
                "USD"
        ));

        Supplier s3 = supplierRepository.save(new Supplier(
                org.getId(),
                "SUP-APEX-03",
                "Apex Office Essentials",
                "US-44719283",
                "invoices@apexoffice.com",
                "77 Commerce Blvd, Chicago, IL",
                "USD"
        ));

        // 5. Purchase Orders
        PurchaseOrder po1 = new PurchaseOrder(org.getId(), s1.getId(), "PO-2026-9001", LocalDate.now().minusDays(10), "USD");
        po1.addLine(new PurchaseOrderLine("High-capacity industrial pump unit", BigDecimal.valueOf(2), new BigDecimal("5000.00"), new BigDecimal("18.00"), new BigDecimal("10000.00")));
        po1.setSubtotal(new BigDecimal("10000.00"));
        po1.setTaxAmount(new BigDecimal("1800.00"));
        po1.setTotalAmount(new BigDecimal("11800.00"));
        po1 = purchaseOrderRepository.save(po1);

        PurchaseOrder po2 = new PurchaseOrder(org.getId(), s2.getId(), "PO-2026-9002", LocalDate.now().minusDays(5), "USD");
        po2.addLine(new PurchaseOrderLine("Dedicated Kubernetes cloud cluster compute", BigDecimal.ONE, new BigDecimal("3000.00"), new BigDecimal("18.00"), new BigDecimal("3000.00")));
        po2.setSubtotal(new BigDecimal("3000.00"));
        po2.setTaxAmount(new BigDecimal("540.00"));
        po2.setTotalAmount(new BigDecimal("3540.00"));
        purchaseOrderRepository.save(po2);

        // 6. Invoices in Realistic States
        // Invoice A: Approved & Exported
        Invoice inv1 = new Invoice(org.getId(), "INV-ACME-2026-001", null);
        inv1.setSupplierId(s1.getId());
        inv1.setPurchaseOrderId(po1.getId());
        inv1.setInvoiceDate(LocalDate.now().minusDays(8));
        inv1.setDueDate(LocalDate.now().plusDays(22));
        inv1.setCurrency("USD");
        inv1.setSubtotal(new BigDecimal("10000.00"));
        inv1.setTaxAmount(new BigDecimal("1800.00"));
        inv1.setTotalAmount(new BigDecimal("11800.00"));
        inv1.setValidationStatus(ValidationStatus.PASSED);
        inv1.setApprovalStatus(ApprovalStatus.APPROVED);
        inv1.setProcessingStatus(ProcessingStatus.EXPORTED);
        inv1.addLine(new InvoiceLine("High-capacity industrial pump unit", BigDecimal.valueOf(2), new BigDecimal("5000.00"), new BigDecimal("18.00"), new BigDecimal("10000.00")));
        invoiceRepository.save(inv1);

        // Invoice B: Pending Approval (Actionable for demo!)
        Invoice inv2 = new Invoice(org.getId(), "INV-CLOUD-2026-442", null);
        inv2.setSupplierId(s2.getId());
        inv2.setPurchaseOrderId(po2.getId());
        inv2.setInvoiceDate(LocalDate.now().minusDays(2));
        inv2.setDueDate(LocalDate.now().plusDays(28));
        inv2.setCurrency("USD");
        inv2.setSubtotal(new BigDecimal("3000.00"));
        inv2.setTaxAmount(new BigDecimal("540.00"));
        inv2.setTotalAmount(new BigDecimal("3540.00"));
        inv2.setValidationStatus(ValidationStatus.PASSED);
        inv2.setApprovalStatus(ApprovalStatus.PENDING);
        inv2.setProcessingStatus(ProcessingStatus.PENDING_APPROVAL);
        inv2.addLine(new InvoiceLine("Dedicated Kubernetes cloud cluster compute", BigDecimal.ONE, new BigDecimal("3000.00"), new BigDecimal("18.00"), new BigDecimal("3000.00")));
        inv2 = invoiceRepository.save(inv2);

        ApprovalTask task = new ApprovalTask(org.getId(), null, null, inv2.getId(), approver.getId(), "APPROVER");
        approvalTaskRepository.save(task);

        // Invoice C: Needs Review (Math Mismatch Exception)
        Invoice inv3 = new Invoice(org.getId(), "INV-APEX-9912", null);
        inv3.setSupplierId(s3.getId());
        inv3.setInvoiceDate(LocalDate.now().minusDays(1));
        inv3.setDueDate(LocalDate.now().plusDays(14));
        inv3.setCurrency("USD");
        inv3.setSubtotal(new BigDecimal("450.00"));
        inv3.setTaxAmount(new BigDecimal("50.00"));
        inv3.setTotalAmount(new BigDecimal("600.00")); // Stated 600 vs expected 500
        inv3.setValidationStatus(ValidationStatus.FAILED);
        inv3.setApprovalStatus(ApprovalStatus.NOT_REQUIRED);
        inv3.setProcessingStatus(ProcessingStatus.NEEDS_REVIEW);
        inv3.addLine(new InvoiceLine("Ergonomic office mesh chairs", BigDecimal.valueOf(3), new BigDecimal("150.00"), BigDecimal.ZERO, new BigDecimal("450.00")));
        invoiceRepository.save(inv3);

        log.info("Operational AP data seeded successfully: 4 users, 3 suppliers, 2 POs, 3 invoices, 1 pending approval task.");
    }
}
