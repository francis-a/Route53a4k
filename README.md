# Route53a4k

**Route53a4k** is a Kotlin-based command-line application designed to dynamically update an AWS Route 53 DNS record to
match your current host's IP address.

## 🚀Download and Run

Select the latest release from GitHub; each release contains two ways to run the application. 

The first is a `jar` that can be run directly.

The second is a `zip` or `tar` that can be extracted.
Inside there is a `bin` folder with a shell script or `bat` file that can be used to run the application.

## 🔧 Features

- **Initialize Configuration**
  Set up your Route 53 Hosted Zone ID, domain name, and AWS credentials using the `init` command.
- **Immediate Update**
  Run `run` to immediately verify and update the Route 53 A record for your configured domain.
- **Scheduled Updates**
  Use the `schedule` command with a valid cron expression to continuously verify and update your DNS record while the
  app is running.

## 💡 Usage Overview

```
# Initialize configuration
route53a4k init

# Immediately run update
route53a4k run

# Schedule recurring updates
route53a4k schedule
```

All commands are interactive and guide you through the required inputs like:

- Hosted Zone ID
- Hostname (DNS record to update)
- AWS credentials (or use the default AWS provider chain)
- Schedule (using a standard cron expression)

## 🔐 Required IAM Permissions

To interact with Route 53, your AWS credentials must allow:

```
route53:ChangeResourceRecordSets
route53:ListResourceRecordSets
```

A CloudFormation template is included in the project for creating a user and access key with the required permissions.