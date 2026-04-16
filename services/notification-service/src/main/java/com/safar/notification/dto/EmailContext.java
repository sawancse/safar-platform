package com.safar.notification.dto;

import java.util.List;
import java.util.Map;

public class EmailContext {
    // Guest info
    private String guestName;
    private String guestEmail;
    private String guestCity;
    private String guestLanguage;

    // Host info
    private String hostName;
    private String hostEmail;
    private String hostPhone;
    private boolean starHost;
    private String hostAvatarUrl;

    // Booking info
    private String bookingRef;
    private String checkIn;
    private String checkOut;
    private String arrivalTime;
    private int nights;
    private int rooms;
    private int guests;
    private int adults;
    private int children;
    private int infants;
    private String bookingType;
    private String specialRequests;

    // Listing info
    private String listingTitle;
    private String listingType;
    private String listingCity;
    private String listingState;
    private String listingAddress;
    private String listingImageUrl;
    private String checkInTime;
    private String checkOutTime;
    private List<String> amenities;
    private String discoveryCategories;
    private String pricingUnit;

    // Pricing (formatted in rupees)
    private String totalAmount;
    private String baseAmount;
    private String gstAmount;
    private String cleaningFee;
    private String platformFee;
    private String nonRefundableDiscount;
    private boolean nonRefundable;
    private String paymentMode;
    private String prepaidAmount;
    private String dueAtProperty;
    private String securityDeposit;
    private String insuranceAmount;
    private String inclusionsTotal;

    // Chapter info
    private int chapterNumber;
    private String chapterTitle;
    private int totalChapters;

    // Tone
    private String tone; // FORMAL or CASUAL

    // Dynamic content
    private String countdownText;
    private String weatherForecast;
    private List<String> localHighlights;
    private List<String> nearbyAttractions;
    private String transitInfo;
    private List<String> localPhrases;
    private boolean isOutstationGuest;

    // Host earnings
    private String monthlyEarnings;
    private String commissionPaid;
    private String netPayout;
    private String occupancyRate;
    private String commissionTier;
    private String nextTierSavings;
    private int totalBookings;
    private String avgRating;
    private String starHostProgress;

    // Milestones
    private String milestoneTitle;
    private String milestoneDescription;
    private String milestoneBadge;
    private int milestoneValue;
    private int nextMilestoneValue;

    // Festival
    private String festivalName;
    private String festivalHeadline;
    private String festivalBody;

    // Review prompt
    private String reviewUrl;

    // Re-engagement
    private String lastCity;
    private int daysSinceLastStay;

    // Mid-stay
    private String feedbackUrl;
    private String issueReportUrl;

    // Medical
    private String procedureName;
    private String hospitalName;
    private String procedureDate;

    // Cancellation
    private String cancellationReason;
    private String refundAmount;
    private String refundTimeline;

    // URLs
    private String bookingUrl;
    private String dashboardUrl;
    private String unsubscribeUrl;
    private String preferencesUrl;

    // Safar Cooks fields
    private String chefName;
    private String chefRating;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String serviceDate;
    private String serviceTime;
    private String mealType;
    private int guestsCount;
    private String cuisineType;
    private String address;
    private String city;
    private String locality;
    private String chefEarnings;
    private String eventType;
    private String eventTime;
    private int eventGuestCount;
    private String menuDescription;
    private String decorationAmount;
    private String cakeAmount;
    private String staffAmount;
    private String advanceAmount;
    private String balanceAmount;
    private String venueAddress;
    private String duration;
    private int guestCount;
    private String subscriptionPlan;
    private String monthlyAmount;
    private String startDate;
    private String endDate;
    private String schedule;
    private String repeatBookUrl;

    // Extra dynamic data
    private Map<String, Object> extras;

    // Donation fields
    private String donationRef;
    private String donorName;
    private String donorEmail;
    private String donationAmount;
    private String donationFrequency;
    private String receiptNumber;
    private String donorPan;
    private String dedicatedTo;
    private String donationTier;
    private String taxSaving;

    // PG Tenancy fields
    private String tenancyRef;
    private String invoiceNumber;
    private String rentAmount;        // formatted e.g. "₹8,000"
    private String dueDate;
    private int daysUntilDue;
    private int daysOverdue;
    private String penaltyAmount;
    private String agreementNumber;
    private String agreementUrl;
    private String propertyName;
    private String roomDescription;
    private String moveInDate;
    private String leaseMonths;
    private String moveOutDate;
    private String noticePeriodDays;

    // Flight fields
    private String bookingRef;
    private String flightRoute;
    private String flightDate;
    private String airline;
    private String flightNumber;
    private String totalAmount;
    private String refundAmount;
    private boolean isInternational;

    // Flight getters and setters
    public String getBookingRef() { return bookingRef; }
    public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }
    public String getFlightRoute() { return flightRoute; }
    public void setFlightRoute(String flightRoute) { this.flightRoute = flightRoute; }
    public String getFlightDate() { return flightDate; }
    public void setFlightDate(String flightDate) { this.flightDate = flightDate; }
    public String getAirline() { return airline; }
    public void setAirline(String airline) { this.airline = airline; }
    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
    public String getTotalAmount() { return totalAmount; }
    public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }
    public String getRefundAmount() { return refundAmount; }
    public void setRefundAmount(String refundAmount) { this.refundAmount = refundAmount; }
    public boolean getIsInternational() { return isInternational; }
    public void setIsInternational(boolean isInternational) { this.isInternational = isInternational; }

    // Getters and setters
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }
    public String getGuestCity() { return guestCity; }
    public void setGuestCity(String guestCity) { this.guestCity = guestCity; }
    public String getGuestLanguage() { return guestLanguage; }
    public void setGuestLanguage(String guestLanguage) { this.guestLanguage = guestLanguage; }
    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
    public String getHostEmail() { return hostEmail; }
    public void setHostEmail(String hostEmail) { this.hostEmail = hostEmail; }
    public String getHostPhone() { return hostPhone; }
    public void setHostPhone(String hostPhone) { this.hostPhone = hostPhone; }
    public boolean isStarHost() { return starHost; }
    public void setStarHost(boolean starHost) { this.starHost = starHost; }
    public String getHostAvatarUrl() { return hostAvatarUrl; }
    public void setHostAvatarUrl(String hostAvatarUrl) { this.hostAvatarUrl = hostAvatarUrl; }
    public String getBookingRef() { return bookingRef; }
    public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }
    public String getCheckIn() { return checkIn; }
    public void setCheckIn(String checkIn) { this.checkIn = checkIn; }
    public String getCheckOut() { return checkOut; }
    public void setCheckOut(String checkOut) { this.checkOut = checkOut; }
    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }
    public int getNights() { return nights; }
    public void setNights(int nights) { this.nights = nights; }
    public int getRooms() { return rooms; }
    public void setRooms(int rooms) { this.rooms = rooms; }
    public int getGuests() { return guests; }
    public void setGuests(int guests) { this.guests = guests; }
    public int getAdults() { return adults; }
    public void setAdults(int adults) { this.adults = adults; }
    public int getChildren() { return children; }
    public void setChildren(int children) { this.children = children; }
    public int getInfants() { return infants; }
    public void setInfants(int infants) { this.infants = infants; }
    public String getBookingType() { return bookingType; }
    public void setBookingType(String bookingType) { this.bookingType = bookingType; }
    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }
    public String getListingTitle() { return listingTitle; }
    public void setListingTitle(String listingTitle) { this.listingTitle = listingTitle; }
    public String getListingType() { return listingType; }
    public void setListingType(String listingType) { this.listingType = listingType; }
    public String getListingCity() { return listingCity; }
    public void setListingCity(String listingCity) { this.listingCity = listingCity; }
    public String getListingState() { return listingState; }
    public void setListingState(String listingState) { this.listingState = listingState; }
    public String getListingAddress() { return listingAddress; }
    public void setListingAddress(String listingAddress) { this.listingAddress = listingAddress; }
    public String getListingImageUrl() { return listingImageUrl; }
    public void setListingImageUrl(String listingImageUrl) { this.listingImageUrl = listingImageUrl; }
    public String getCheckInTime() { return checkInTime; }
    public void setCheckInTime(String checkInTime) { this.checkInTime = checkInTime; }
    public String getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(String checkOutTime) { this.checkOutTime = checkOutTime; }
    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }
    public String getDiscoveryCategories() { return discoveryCategories; }
    public void setDiscoveryCategories(String discoveryCategories) { this.discoveryCategories = discoveryCategories; }
    public String getPricingUnit() { return pricingUnit; }
    public void setPricingUnit(String pricingUnit) { this.pricingUnit = pricingUnit; }
    public String getTotalAmount() { return totalAmount; }
    public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }
    public String getBaseAmount() { return baseAmount; }
    public void setBaseAmount(String baseAmount) { this.baseAmount = baseAmount; }
    public String getGstAmount() { return gstAmount; }
    public void setGstAmount(String gstAmount) { this.gstAmount = gstAmount; }
    public String getCleaningFee() { return cleaningFee; }
    public void setCleaningFee(String cleaningFee) { this.cleaningFee = cleaningFee; }
    public String getPlatformFee() { return platformFee; }
    public void setPlatformFee(String platformFee) { this.platformFee = platformFee; }
    public String getSecurityDeposit() { return securityDeposit; }
    public void setSecurityDeposit(String securityDeposit) { this.securityDeposit = securityDeposit; }
    public String getInsuranceAmount() { return insuranceAmount; }
    public void setInsuranceAmount(String insuranceAmount) { this.insuranceAmount = insuranceAmount; }
    public String getInclusionsTotal() { return inclusionsTotal; }
    public void setInclusionsTotal(String inclusionsTotal) { this.inclusionsTotal = inclusionsTotal; }
    public String getNonRefundableDiscount() { return nonRefundableDiscount; }
    public void setNonRefundableDiscount(String nonRefundableDiscount) { this.nonRefundableDiscount = nonRefundableDiscount; }
    public boolean isNonRefundable() { return nonRefundable; }
    public void setNonRefundable(boolean nonRefundable) { this.nonRefundable = nonRefundable; }
    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
    public String getPrepaidAmount() { return prepaidAmount; }
    public void setPrepaidAmount(String prepaidAmount) { this.prepaidAmount = prepaidAmount; }
    public String getDueAtProperty() { return dueAtProperty; }
    public void setDueAtProperty(String dueAtProperty) { this.dueAtProperty = dueAtProperty; }
    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int chapterNumber) { this.chapterNumber = chapterNumber; }
    public String getChapterTitle() { return chapterTitle; }
    public void setChapterTitle(String chapterTitle) { this.chapterTitle = chapterTitle; }
    public int getTotalChapters() { return totalChapters; }
    public void setTotalChapters(int totalChapters) { this.totalChapters = totalChapters; }
    public String getTone() { return tone != null ? tone : "FORMAL"; }
    public void setTone(String tone) { this.tone = tone; }
    // Alias: templates use ctx.city
    public String getCity() { return listingCity; }
    public String getCountdownText() { return countdownText; }
    public void setCountdownText(String countdownText) { this.countdownText = countdownText; }
    public String getWeatherForecast() { return weatherForecast; }
    public void setWeatherForecast(String weatherForecast) { this.weatherForecast = weatherForecast; }
    public List<String> getLocalHighlights() { return localHighlights; }
    public void setLocalHighlights(List<String> localHighlights) { this.localHighlights = localHighlights; }
    public List<String> getNearbyAttractions() { return nearbyAttractions; }
    public void setNearbyAttractions(List<String> nearbyAttractions) { this.nearbyAttractions = nearbyAttractions; }
    public String getTransitInfo() { return transitInfo; }
    public void setTransitInfo(String transitInfo) { this.transitInfo = transitInfo; }
    public List<String> getLocalPhrases() { return localPhrases; }
    public void setLocalPhrases(List<String> localPhrases) { this.localPhrases = localPhrases; }
    public boolean isOutstationGuest() { return isOutstationGuest; }
    public void setOutstationGuest(boolean outstationGuest) { isOutstationGuest = outstationGuest; }
    public String getMonthlyEarnings() { return monthlyEarnings; }
    public void setMonthlyEarnings(String monthlyEarnings) { this.monthlyEarnings = monthlyEarnings; }
    public String getCommissionPaid() { return commissionPaid; }
    public void setCommissionPaid(String commissionPaid) { this.commissionPaid = commissionPaid; }
    public String getNetPayout() { return netPayout; }
    public void setNetPayout(String netPayout) { this.netPayout = netPayout; }
    public String getOccupancyRate() { return occupancyRate; }
    public void setOccupancyRate(String occupancyRate) { this.occupancyRate = occupancyRate; }
    public String getCommissionTier() { return commissionTier; }
    public void setCommissionTier(String commissionTier) { this.commissionTier = commissionTier; }
    public String getNextTierSavings() { return nextTierSavings; }
    public void setNextTierSavings(String nextTierSavings) { this.nextTierSavings = nextTierSavings; }
    public int getTotalBookings() { return totalBookings; }
    public void setTotalBookings(int totalBookings) { this.totalBookings = totalBookings; }
    public String getAvgRating() { return avgRating; }
    public void setAvgRating(String avgRating) { this.avgRating = avgRating; }
    public String getStarHostProgress() { return starHostProgress; }
    public void setStarHostProgress(String starHostProgress) { this.starHostProgress = starHostProgress; }
    public String getMilestoneTitle() { return milestoneTitle; }
    public void setMilestoneTitle(String milestoneTitle) { this.milestoneTitle = milestoneTitle; }
    public String getMilestoneDescription() { return milestoneDescription; }
    public void setMilestoneDescription(String milestoneDescription) { this.milestoneDescription = milestoneDescription; }
    public String getMilestoneBadge() { return milestoneBadge; }
    public void setMilestoneBadge(String milestoneBadge) { this.milestoneBadge = milestoneBadge; }
    public int getMilestoneValue() { return milestoneValue; }
    public void setMilestoneValue(int milestoneValue) { this.milestoneValue = milestoneValue; }
    public int getNextMilestoneValue() { return nextMilestoneValue; }
    public void setNextMilestoneValue(int nextMilestoneValue) { this.nextMilestoneValue = nextMilestoneValue; }
    public String getFestivalName() { return festivalName; }
    public void setFestivalName(String festivalName) { this.festivalName = festivalName; }
    public String getFestivalHeadline() { return festivalHeadline; }
    public void setFestivalHeadline(String festivalHeadline) { this.festivalHeadline = festivalHeadline; }
    public String getFestivalBody() { return festivalBody; }
    public void setFestivalBody(String festivalBody) { this.festivalBody = festivalBody; }
    public String getReviewUrl() { return reviewUrl; }
    public void setReviewUrl(String reviewUrl) { this.reviewUrl = reviewUrl; }
    public String getLastCity() { return lastCity; }
    public void setLastCity(String lastCity) { this.lastCity = lastCity; }
    public int getDaysSinceLastStay() { return daysSinceLastStay; }
    public void setDaysSinceLastStay(int daysSinceLastStay) { this.daysSinceLastStay = daysSinceLastStay; }
    public String getFeedbackUrl() { return feedbackUrl; }
    public void setFeedbackUrl(String feedbackUrl) { this.feedbackUrl = feedbackUrl; }
    public String getIssueReportUrl() { return issueReportUrl; }
    public void setIssueReportUrl(String issueReportUrl) { this.issueReportUrl = issueReportUrl; }
    public String getProcedureName() { return procedureName; }
    public void setProcedureName(String procedureName) { this.procedureName = procedureName; }
    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
    public String getProcedureDate() { return procedureDate; }
    public void setProcedureDate(String procedureDate) { this.procedureDate = procedureDate; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public String getRefundAmount() { return refundAmount; }
    public void setRefundAmount(String refundAmount) { this.refundAmount = refundAmount; }
    public String getRefundTimeline() { return refundTimeline; }
    public void setRefundTimeline(String refundTimeline) { this.refundTimeline = refundTimeline; }
    public String getBookingUrl() { return bookingUrl; }
    public void setBookingUrl(String bookingUrl) { this.bookingUrl = bookingUrl; }
    private String paymentUrl;
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
    public String getDashboardUrl() { return dashboardUrl; }
    public void setDashboardUrl(String dashboardUrl) { this.dashboardUrl = dashboardUrl; }
    public String getUnsubscribeUrl() { return unsubscribeUrl; }
    public void setUnsubscribeUrl(String unsubscribeUrl) { this.unsubscribeUrl = unsubscribeUrl; }
    public String getPreferencesUrl() { return preferencesUrl; }
    public void setPreferencesUrl(String preferencesUrl) { this.preferencesUrl = preferencesUrl; }
    public Map<String, Object> getExtras() { return extras; }
    public void setExtras(Map<String, Object> extras) { this.extras = extras; }

    // ── Template aliases (templates use ctx.X, context stores as different field name) ──
    public String getUserName() { return guestName; }
    public String getCheckInDate() { return checkIn; }
    public String getCheckOutDate() { return checkOut; }
    public int getDaysUntilCheckin() {
        if (checkIn == null) return 0;
        try {
            long diff = java.time.LocalDate.parse(checkIn.length() > 10 ? checkIn.substring(0, 10) : checkIn)
                    .toEpochDay() - java.time.LocalDate.now().toEpochDay();
            return (int) Math.max(0, diff);
        } catch (Exception e) { return 0; }
    }
    public String getStayDuration() { return nights + " night" + (nights != 1 ? "s" : ""); }
    public String getSubject() { return "Booking " + (bookingRef != null ? bookingRef : ""); }
    public String getSupportUrl() { return "https://ysafar.com/support"; }
    public String getSupportPhone() { return "+91-1800-SAFAR"; }
    public String getCompanyAddress() { return "Safar Technologies, India"; }
    public String getExploreUrl() { return dashboardUrl != null ? dashboardUrl : "https://ysafar.com"; }
    public String getRebookUrl() { return dashboardUrl != null ? dashboardUrl : "https://ysafar.com"; }
    public String getEmailPreferencesUrl() { return preferencesUrl != null ? preferencesUrl : "https://ysafar.com/dashboard/account"; }
    public String getPriceLabel() {
        if ("MONTH".equals(pricingUnit)) return "per month";
        if ("HOUR".equals(pricingUnit)) return "per hour";
        return "per night";
    }
    /** Line-item label for the base stay charge in price breakdown — includes duration. */
    public String getStayChargesLabel() {
        if ("MONTH".equals(pricingUnit)) {
            long fullMonths = nights / 30;
            long rem = nights % 30;
            if (fullMonths == 0) return "Rent (" + nights + " day" + (nights != 1 ? "s" : "") + ")";
            StringBuilder s = new StringBuilder("Rent (").append(fullMonths)
                    .append(" month").append(fullMonths != 1 ? "s" : "");
            if (rem > 0) s.append(" + ").append(rem).append(" day").append(rem != 1 ? "s" : "");
            return s.append(")").toString();
        }
        if ("HOUR".equals(pricingUnit)) {
            return "Stay charges";
        }
        return "Stay charges (" + nights + " night" + (nights != 1 ? "s" : "") + ")";
    }
    /** True when this booking is cash/pay-at-property and not yet paid online. */
    public boolean getIsCashPayment() {
        return "PAY_AT_PROPERTY".equals(paymentMode);
    }
    /** Deep link to the booking page with a flag that opens Razorpay to retry payment. */
    public String getPayNowUrl() {
        if (bookingUrl == null) return null;
        return bookingUrl + (bookingUrl.contains("?") ? "&" : "?") + "payNow=1";
    }
    public String getTransactionRef() { return bookingRef; }
    public String getRecipientEmail() { return guestEmail; }
    public double getAverageRating() { return avgRating != null ? Double.parseDouble(avgRating) : 0; }
    public int getTotalNights() { return nights; }
    public String getTotalEarnings() { return netPayout; }
    public String getTotalCommission() { return commissionPaid; }
    public String getCommissionRate() { return commissionTier; }
    public String getReportMonth() {
        return java.time.YearMonth.now().toString();
    }
    public double getStarHostProgressPercent() {
        try { return starHostProgress != null ? Double.parseDouble(starHostProgress) : 0; }
        catch (Exception e) { return 0; }
    }
    public String getStarHostProgressText() { return starHostProgress != null ? starHostProgress + "%" : "0%"; }
    public String getTierUpgradeSaving() { return nextTierSavings; }
    public String getUpgradeTierUrl() { return "https://ysafar.com/host?tab=subscription"; }
    public String getReceiptDownloadUrl() { return bookingUrl; }
    public String getRefundMethod() { return "Original payment method"; }
    public String getBadgeName() { return milestoneBadge; }
    public String getBadgeEmoji() {
        if (milestoneBadge == null) return "🏆";
        return switch (milestoneBadge) {
            case "Explorer" -> "🧭";
            case "Wanderer" -> "🌍";
            case "Voyager" -> "🚀";
            case "Globetrotter" -> "✈️";
            default -> "🏆";
        };
    }
    public String getSpecialOfferText() { return null; }
    public String getSpecialOfferCode() { return null; }
    public String getSpecialOfferExpiry() { return null; }
    public String getReturningGuestDiscount() { return null; }
    public String getWeatherHint() { return weatherForecast; }
    public List<String> getArrivalTips() { return localHighlights; }
    public List<String> getRecommendedActivities() { return nearbyAttractions; }
    public List<String> getRecommendations() { return nearbyAttractions; }
    public List<String> getFeaturedStays() { return null; }
    public List<String> getDiscoveryTeasers() { return null; }
    public String getRoommateName() { return null; }
    public String getProcedureReminder() { return procedureName; }
    public String getCampaignHeadline() { return festivalHeadline; }
    public String getCampaignBody() { return festivalBody; }
    public String getFestivalGreeting() { return festivalHeadline; }
    public String getFestivalEmojis() { return "🎉🪔✨"; }
    public String getFestivalGradient() { return "linear-gradient(135deg, #FF5A5F, #FF8C69)"; }
    public String getFestivalImageUrl() { return null; }
    public String getServiceFee() { return platformFee; }
    public String getBookingAmount() { return totalAmount; }
    public String getHelpUrl() { return "https://ysafar.com/help"; }
    public String getHostResponseRate() { return "95%"; }
    public int getHoursUntilCheckin() { return getDaysUntilCheckin() * 24; }
    public int getMinsUntilCheckin() { return getHoursUntilCheckin() * 60; }
    public boolean getIsStarHost() { return starHost; }

    // ── Safar Cooks getters/setters ──
    public String getChefName() { return chefName; }
    public void setChefName(String chefName) { this.chefName = chefName; }
    public String getChefRating() { return chefRating; }
    public void setChefRating(String chefRating) { this.chefRating = chefRating; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerEmailAddr() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getServiceDate() { return serviceDate; }
    public void setServiceDate(String serviceDate) { this.serviceDate = serviceDate; }
    public String getServiceTime() { return serviceTime; }
    public void setServiceTime(String serviceTime) { this.serviceTime = serviceTime; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public int getGuestsCount() { return guestsCount; }
    public void setGuestsCount(int guestsCount) { this.guestsCount = guestsCount; }
    public String getCuisineType() { return cuisineType; }
    public void setCuisineType(String cuisineType) { this.cuisineType = cuisineType; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getChefCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }
    public String getChefEarnings() { return chefEarnings; }
    public void setChefEarnings(String chefEarnings) { this.chefEarnings = chefEarnings; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getEventTime() { return eventTime; }
    public void setEventTime(String eventTime) { this.eventTime = eventTime; }
    public int getEventGuestCount() { return eventGuestCount; }
    public void setEventGuestCount(int eventGuestCount) { this.eventGuestCount = eventGuestCount; }
    public String getMenuDescription() { return menuDescription; }
    public void setMenuDescription(String menuDescription) { this.menuDescription = menuDescription; }
    public String getDecorationAmount() { return decorationAmount; }
    public void setDecorationAmount(String decorationAmount) { this.decorationAmount = decorationAmount; }
    public String getCakeAmount() { return cakeAmount; }
    public void setCakeAmount(String cakeAmount) { this.cakeAmount = cakeAmount; }
    public String getStaffAmount() { return staffAmount; }
    public void setStaffAmount(String staffAmount) { this.staffAmount = staffAmount; }
    public String getAdvanceAmount() { return advanceAmount; }
    public void setAdvanceAmount(String advanceAmount) { this.advanceAmount = advanceAmount; }
    public String getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(String balanceAmount) { this.balanceAmount = balanceAmount; }
    public String getVenueAddress() { return venueAddress; }
    public void setVenueAddress(String venueAddress) { this.venueAddress = venueAddress; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public int getGuestCount() { return guestCount; }
    public void setGuestCount(int guestCount) { this.guestCount = guestCount; }
    public String getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(String subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }
    public String getMonthlyAmount() { return monthlyAmount; }
    public void setMonthlyAmount(String monthlyAmount) { this.monthlyAmount = monthlyAmount; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }
    public String getRepeatBookUrl() { return repeatBookUrl != null ? repeatBookUrl : "https://ysafar.com/cooks"; }
    public void setRepeatBookUrl(String repeatBookUrl) { this.repeatBookUrl = repeatBookUrl; }
    public String getCooksUrl() { return "https://ysafar.com/cooks"; }
    public String getCooksBookingsUrl() { return "https://ysafar.com/cooks/my-bookings"; }

    // ── Donation getters/setters ──
    public String getDonationRef() { return donationRef; }
    public void setDonationRef(String donationRef) { this.donationRef = donationRef; }
    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }
    public String getDonorEmail() { return donorEmail; }
    public void setDonorEmail(String donorEmail) { this.donorEmail = donorEmail; }
    public String getDonationAmount() { return donationAmount; }
    public void setDonationAmount(String donationAmount) { this.donationAmount = donationAmount; }
    public String getDonationFrequency() { return donationFrequency; }
    public void setDonationFrequency(String donationFrequency) { this.donationFrequency = donationFrequency; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public String getDonorPan() { return donorPan; }
    public void setDonorPan(String donorPan) { this.donorPan = donorPan; }
    public String getDedicatedTo() { return dedicatedTo; }
    public void setDedicatedTo(String dedicatedTo) { this.dedicatedTo = dedicatedTo; }
    public String getDonationTier() { return donationTier; }
    public void setDonationTier(String donationTier) { this.donationTier = donationTier; }
    public String getTaxSaving() { return taxSaving; }
    public void setTaxSaving(String taxSaving) { this.taxSaving = taxSaving; }
    public String getMaskedPan() {
        if (donorPan == null || donorPan.length() < 4) return donorPan;
        return "XXXXX" + donorPan.substring(donorPan.length() - 4) + "X";
    }
    public String getDonationImpactUrl() { return "https://ysafar.com/aashray/impact"; }

    // Template aliases used by payment-receipt.html
    public boolean getIsNonRefundable() { return nonRefundable; }
    public String getPaymentDate() {
        // Fall back to check-in date string if no explicit payment date was set
        return checkIn != null && checkIn.length() >= 10 ? checkIn.substring(0, 10) : checkIn;
    }
    public String getPaymentMethod() {
        if (paymentMode == null) return null;
        return switch (paymentMode) {
            case "PREPAID" -> "Online payment";
            case "PAY_AT_PROPERTY" -> "Pay at property";
            case "PARTIAL_PREPAID" -> "Partial online payment";
            default -> paymentMode;
        };
    }

    // PG Tenancy getters/setters
    public String getTenancyRef() { return tenancyRef; }
    public void setTenancyRef(String tenancyRef) { this.tenancyRef = tenancyRef; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getRentAmount() { return rentAmount; }
    public void setRentAmount(String rentAmount) { this.rentAmount = rentAmount; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public int getDaysUntilDue() { return daysUntilDue; }
    public void setDaysUntilDue(int daysUntilDue) { this.daysUntilDue = daysUntilDue; }
    public int getDaysOverdue() { return daysOverdue; }
    public void setDaysOverdue(int daysOverdue) { this.daysOverdue = daysOverdue; }
    public String getPenaltyAmount() { return penaltyAmount; }
    public void setPenaltyAmount(String penaltyAmount) { this.penaltyAmount = penaltyAmount; }
    public String getAgreementNumber() { return agreementNumber; }
    public void setAgreementNumber(String agreementNumber) { this.agreementNumber = agreementNumber; }
    public String getAgreementUrl() { return agreementUrl; }
    public void setAgreementUrl(String agreementUrl) { this.agreementUrl = agreementUrl; }
    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }
    public String getRoomDescription() { return roomDescription; }
    public void setRoomDescription(String roomDescription) { this.roomDescription = roomDescription; }
    public String getMoveInDate() { return moveInDate; }
    public void setMoveInDate(String moveInDate) { this.moveInDate = moveInDate; }
    public String getLeaseMonths() { return leaseMonths; }
    public void setLeaseMonths(String leaseMonths) { this.leaseMonths = leaseMonths; }
    public String getMoveOutDate() { return moveOutDate; }
    public void setMoveOutDate(String moveOutDate) { this.moveOutDate = moveOutDate; }
    public String getNoticePeriodDays() { return noticePeriodDays; }
    public void setNoticePeriodDays(String noticePeriodDays) { this.noticePeriodDays = noticePeriodDays; }

    // Computed map URL for check-in email (Google Maps search link)
    public String getMapUrl() {
        StringBuilder q = new StringBuilder();
        if (listingAddress != null && !listingAddress.isBlank()) q.append(listingAddress);
        if (listingCity != null && !listingCity.isBlank()) {
            if (q.length() > 0) q.append(", ");
            q.append(listingCity);
        }
        if (listingState != null && !listingState.isBlank()) {
            if (q.length() > 0) q.append(", ");
            q.append(listingState);
        }
        if (q.length() == 0 && listingTitle != null) q.append(listingTitle);
        if (q.length() == 0) return "#";
        return "https://www.google.com/maps/search/?api=1&query="
                + java.net.URLEncoder.encode(q.toString(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
